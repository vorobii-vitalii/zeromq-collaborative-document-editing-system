import { Path } from "./Path";
import { Change } from "../document/editing/change";
import { Builder } from "flatbuffers";

export class CharDetails {
  public charId: string;
  public parentCharId: string | undefined;
  private direction: boolean;
  private disambiguator: number;
  private path?: Path;
  public character?: string | null;

  public constructor(
    charId: string,
    parentCharId: string | undefined,
    direction: boolean,
    disambiguator: number,
    character?: string
  ) {
    this.parentCharId = parentCharId;
    this.character = character;
    this.charId = charId;
    this.direction = direction;
    this.disambiguator = disambiguator;
  }

  public static createBetween(
    left: CharDetails | undefined,
    right: CharDetails | undefined,
    character: string,
    disambiguator: number,
    charId: string
  ): CharDetails {
    if (!left && !right) {
      return new CharDetails(
        charId,
        undefined,
        true,
        disambiguator,
        character
      ).updatePath(new Path([], []));
    }
    if (!left) {
      return new CharDetails(
        charId,
        right?.charId,
        false,
        disambiguator,
        character
      ).updatePath(right?.getPath()!!);
    }
    if (!right) {
      return new CharDetails(
        charId,
        left?.charId,
        true,
        disambiguator,
        character
      ).updatePath(left?.getPath()!!);
    }
    if (left.getPath()!!.isAncestorOf(right.getPath()!!)) {
      return new CharDetails(
        charId,
        right?.charId,
        false,
        disambiguator,
        character
      ).updatePath(right?.getPath()!!);
    }
    return new CharDetails(
      charId,
      left?.charId,
      true,
      disambiguator,
      character
    ).updatePath(left?.getPath()!!);
  }

  public applyToByteBuffer(builder: Builder): number {
    const charIdOffset = builder.createString(this.charId);
    const parentCharIdOffset = builder.createString(this.parentCharId);
    return Change.createChange(
      builder,
      charIdOffset,
      parentCharIdOffset,
      this.direction,
      this.disambiguator,
      (this.character && this.character.charCodeAt(0)) || null
    );
  }

  public updateCharacter(character: string | undefined | null) {
    this.character = character;
  }

  public getPath() {
    return this.path;
  }

  public updatePath(parentPath: Path) {
    this.path = parentPath.addAncestor(this.direction, this.disambiguator);
    return this;
  }
}
