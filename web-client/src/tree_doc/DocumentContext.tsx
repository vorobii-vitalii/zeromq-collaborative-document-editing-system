import {CharDetails} from "./CharDetails";
import {Path} from "./Path";
import {DIFF_DELETE, DIFF_EQUAL, DIFF_INSERT, diff_match_patch} from "diff-match-patch";
import { Builder } from "flatbuffers";
import { DocumentElement } from "../document/editing/document-element";
import { ChangeRequest, Request, RequestHolder } from "../document/editing";

const EMPTY_STRING = "";

export class DocumentContext {
    private charDetailsMap = new Map<string, CharDetails>();
    private sortedCharIds = new Array<string>();
    private dependenciesCharIdsByCharId = new Map<string, Set<string>>();
    private previousContent: string = EMPTY_STRING;
    private readonly documentId: number;

    constructor(documentId: number) {
        this.documentId = documentId;
    }

    public applyUserChange(
        updatedDocumentContent: string,
        disambiguator: number,
        changeIdGenerator : () => string
    ) {
        const differences = new diff_match_patch().diff_main(
            this.previousContent,
            updatedDocumentContent
        );
        let previousIndex = -1;
        const changesToApply = new Array<number>();
        const builder = new Builder();
        for (const diff of differences) {
            const v = diff[0];
            const str = diff[1];
            if (v === DIFF_EQUAL) {
                previousIndex += str.length;
            } else if (v === DIFF_DELETE) {
                for (let i = 0; i < str.length; i++) {
                    const charIdToDelete = this.sortedCharIds[previousIndex + 1];
                    const charDetails = this.charDetailsMap.get(charIdToDelete)!!;
                    this.sortedCharIds.splice(previousIndex + 1, 1);
                    charDetails.updateCharacter(undefined);
                    changesToApply.splice(changesToApply.length, 0, charDetails.applyToByteBuffer(builder));
                }
            } else if (v === DIFF_INSERT) {
                const nextCharDetails = this.getCharDetails(previousIndex + 1);
                let previousCharDetails = this.getCharDetails(previousIndex);
                for (let i = 0; i < str.length; i++) {
                    const newCharacter = str.charAt(i);
                    const newCharDetails = CharDetails.createBetween(
                        previousCharDetails,
                        nextCharDetails,
                        newCharacter,
                        disambiguator,
                        changeIdGenerator()
                    );
                    this.charDetailsMap.set(newCharDetails.charId, newCharDetails);
                    changesToApply.splice(changesToApply.length, 0, newCharDetails.applyToByteBuffer(builder));
                    previousCharDetails = newCharDetails;
                    this.sortedCharIds.splice(previousIndex + i + 1, 0, newCharDetails.charId);
                    previousIndex += str.length;
                }
            }
        }
        const changesVectorOffset = ChangeRequest.createChangesVector(builder, changesToApply);
        ChangeRequest.startChangeRequest(builder);
        console.log(`Updating document ${this.documentId}`)
        ChangeRequest.addDocumentId(builder, this.documentId);
        ChangeRequest.addChanges(builder, changesVectorOffset)
        const changeRequestOffset = ChangeRequest.endChangeRequest(builder);
        RequestHolder.startRequestHolder(builder);
        RequestHolder.addRequestType(builder, Request.ChangeRequest);
        RequestHolder.addRequest(builder, changeRequestOffset);
        const requestHolder = RequestHolder.endRequestHolder(builder);
        builder.finish(requestHolder);
        this.previousContent = updatedDocumentContent;
        return builder.asUint8Array();
    }

    public applyExternalChange(change: DocumentElement) {
        // @ts-ignore
        const charId : string = change.charId();
        const isAlreadyPresent = this.charDetailsMap.has(charId);
        if (isAlreadyPresent) {
            this.charDetailsMap.get(charId)!!.updateCharacter(this.extractCharacter(change));
            this.recalculateDocumentContent();
        } else {
            const parentCharId = this.extractParentCharId(change);
            // @ts-ignore
            const d: number = change.disambiguator();
            const charDetails = new CharDetails(
                charId,
                parentCharId,
                change.isRight(),
                d,
                this.extractCharacter(change)
            );
            this.charDetailsMap.set(charId, charDetails);
            const parentPath = this.findPath(parentCharId);
            if (parentPath) {
                this.onPathReady(charId, parentPath);
                this.recalculateDocumentContent();
            } else {
                this.createDependency(charId, parentCharId!!);
            }
        }
    }

    private extractCharacter = (change: DocumentElement): string | undefined => {
        if (change.character() !== 0) {
            return String.fromCodePoint(change.character());
        }
        else {
            return undefined;
        }
    };

    private extractParentCharId = (change: DocumentElement) : string | undefined => {
        if (change.parentCharId() == null) {
            return undefined;
        }
        // @ts-ignore
        return change.parentCharId();
    }

    public getDocumentContent() {
        return this.previousContent;
    }

    private getCharDetails = (index: number) => {
        if (index < 0 || index >= this.sortedCharIds.length) {
            return undefined;
        }
        return this.charDetailsMap.get(this.sortedCharIds[index]);
    };

    private recalculateDocumentContent() {
        this.previousContent = this.sortedCharIds
            .map(v => this.charDetailsMap.get(v))
            .map(v => v && v.character)
            .join(EMPTY_STRING);
    }

    private getPathByIndex = (index: number) => this.charDetailsMap.get(this.sortedCharIds[index])!!.getPath()!!;

    private onPathReady = (charId: string, rootPath: Path) => {
        const queue = new Array<{ charId: string; parentPath: Path }>();
        queue.push({ charId, parentPath: rootPath });
        while (queue.length > 0) {
            const pair = queue.shift();
            if (!pair) {
                continue;
            }
            const charDetails = this.charDetailsMap.get(pair.charId)!!;
            charDetails.updatePath(pair.parentPath);
            // Add to array
            const newCharacterIndex = charDetails.getPath()!!.findOptimalPosition(
                this.sortedCharIds.length,
                this.getPathByIndex
            );
            if (charDetails.character) {
                this.sortedCharIds.splice(newCharacterIndex, 0, pair.charId);
            } else {
                if (
                    newCharacterIndex >= 0 &&
                    newCharacterIndex < this.sortedCharIds.length
                ) {
                    this.sortedCharIds.splice(newCharacterIndex, 1);
                }
            }
            // Update ancestors...
            const dependencies = this.dependenciesCharIdsByCharId.get(pair.charId);
            dependencies && dependencies.forEach(dependencyCharId => {
                queue.push({
                    charId: dependencyCharId,
                    parentPath: charDetails.getPath()!!
                });
            });
            this.dependenciesCharIdsByCharId.delete(pair.charId);
        }
    };

    private findPath = (charId?: string): Path | undefined => {
        if (!charId) {
            return new Path([], []);
        }
        const charDetails = this.charDetailsMap.get(charId);
        return charDetails && charDetails.getPath();
    }

    private createDependency = (dependent: string, dependency: string) => {
        if (!this.dependenciesCharIdsByCharId.has(dependency)) {
            this.dependenciesCharIdsByCharId.set(dependency, new Set<string>());
        }
        const set = this.dependenciesCharIdsByCharId.get(dependency);
        this.dependenciesCharIdsByCharId.set(dependency, set!!.add(dependent));
    };

}