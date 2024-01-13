import { DocumentContext } from "./DocumentContext";
import {
    Change,
    ChangeRequest,
    DocumentElement,
    Request,
    RequestHolder
} from "../document/editing";
import { Builder } from "flatbuffers";

import { TextDecoder, TextEncoder } from 'util';

Object.assign(global, { TextDecoder, TextEncoder });

const CHANGE_ID = "123";
const DISAMBIGUATOR = 2;
const DOC_ID = 12;

const createDocumentElement = (
  charId: string,
  disambiguator: number,
  character: number,
  isRight: boolean,
  parentCharId: string | undefined
) => {
    const builder = new Builder();
    const charIdOffset = builder.createString(charId);
    const parentCharIdOffset =
      (parentCharId && builder.createString(parentCharId)) || 0;
    const offset = DocumentElement.createDocumentElement(
      builder,
      charIdOffset,
      parentCharIdOffset,
      isRight,
      disambiguator,
      character
    );
    builder.finish(offset);
    return DocumentElement.getRootAsDocumentElement(builder.dataBuffer());
};

const createExpectedChangeRequest = (
    changesToApply: [
        {
            charId: string,
            disambiguator: number,
            character: number,
            isRight: boolean,
            parentCharId: string | undefined
        }
    ]
) => {
    const builder = new Builder();
    const changesVectorOffset =
      ChangeRequest.createChangesVector(
        builder,
        changesToApply.map(v => {
            const charIdOffset = builder.createString(v.charId);
            const parentCharIdOffset = builder.createString(v.parentCharId);
            return Change.createChange(
              builder,
              charIdOffset,
              parentCharIdOffset,
              v.isRight,
              v.disambiguator,
              v.character
            );
        })
      );
    ChangeRequest.startChangeRequest(builder);
    ChangeRequest.addDocumentId(builder, DOC_ID);
    ChangeRequest.addChanges(builder, changesVectorOffset)
    const changeRequestOffset = ChangeRequest.endChangeRequest(builder);
    RequestHolder.startRequestHolder(builder);
    RequestHolder.addRequestType(builder, Request.ChangeRequest);
    RequestHolder.addRequest(builder, changeRequestOffset);
    const requestHolder = RequestHolder.endRequestHolder(builder);
    builder.finish(requestHolder);
    return builder.asUint8Array();
}

test('applyUserChange_InsertionEnd', () => {
    const documentContext = new DocumentContext(DOC_ID);
    documentContext.applyExternalChange(createDocumentElement(
        "1",
        1,
        'B'.charCodeAt(0),
        false,
        undefined
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "3",
      1,
      'A'.charCodeAt(0),
      false,
      "1"
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "2",
      1,
      'C'.charCodeAt(0),
      true,
      "1"
    ));

    // User change
    const changesToApply =
        documentContext.applyUserChange("ABCX", DISAMBIGUATOR, () => CHANGE_ID);

    expect(changesToApply).toEqual(createExpectedChangeRequest(
      [
          {
              charId: CHANGE_ID,
              disambiguator: DISAMBIGUATOR,
              character: 'X'.charCodeAt(0),
              parentCharId: '2',
              isRight: true
          }
      ]
    ))
    expect(documentContext.getDocumentContent()).toEqual("ABCX");
})

test('applyUserChange_InsertionBeginning', () => {
    const documentContext = new DocumentContext(DOC_ID);
    documentContext.applyExternalChange(createDocumentElement(
      "1",
      1,
      'B'.charCodeAt(0),
      false,
      undefined
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "3",
      1,
      'A'.charCodeAt(0),
      false,
      "1"
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "2",
      1,
      'C'.charCodeAt(0),
      true,
      "1"
    ));
    // User change
    const changesToApply =
        documentContext.applyUserChange("XABC", DISAMBIGUATOR, () => CHANGE_ID);

    expect(documentContext.getDocumentContent()).toEqual("XABC");
    expect(changesToApply).toEqual(createExpectedChangeRequest(
      [
          {
              charId: CHANGE_ID,
              disambiguator: DISAMBIGUATOR,
              character: 'X'.charCodeAt(0),
              parentCharId: '3',
              isRight: false
          }
      ]
    ))
});

test('applyUserChange_DeletionCase', () => {
    const documentContext = new DocumentContext(DOC_ID);
    documentContext.applyExternalChange(createDocumentElement(
      "1",
      1,
      'B'.charCodeAt(0),
      false,
      undefined
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "3",
      1,
      'A'.charCodeAt(0),
      false,
      "1"
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "2",
      1,
      'C'.charCodeAt(0),
      true,
      "1"
    ));

    // User change
    const changesToApply =
        documentContext.applyUserChange("BC", DISAMBIGUATOR, () => CHANGE_ID);

    expect(documentContext.getDocumentContent()).toEqual("BC");
    expect(changesToApply).toEqual(createExpectedChangeRequest(
      [
          {
              charId: '3',
              isRight: false,
              parentCharId: "1",
              disambiguator: 1,
              character: 0
          }
      ]
    ))
});

test('applyExternalChange_EventualDeletion', () => {
    const documentContext = new DocumentContext(DOC_ID);
    documentContext.applyExternalChange(createDocumentElement(
      "1",
      1,
      'B'.charCodeAt(0),
      false,
      undefined
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "3",
      1,
      'A'.charCodeAt(0),
      false,
      "1"
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "2",
      1,
      'C'.charCodeAt(0),
      true,
      "1"
    ));

    expect(documentContext.getDocumentContent()).toEqual("ABC");
    documentContext.applyExternalChange(createDocumentElement(
      "3",
      1,
      0,
      false,
      "1"
    ));
    expect(documentContext.getDocumentContent()).toEqual("BC");
})

test('applyExternalChange_CorrectOrderingOfChanges', () => {
    const documentContext = new DocumentContext(DOC_ID);
    documentContext.applyExternalChange(createDocumentElement(
      "1",
      1,
      'B'.charCodeAt(0),
      false,
      undefined
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "3",
      1,
      'A'.charCodeAt(0),
      false,
      "1"
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "2",
      1,
      'C'.charCodeAt(0),
      true,
      "1"
    ));
    expect(documentContext.getDocumentContent()).toEqual("ABC");
});

test('applyExternalChange_WrongChangeOrdering', () => {
    const documentContext = new DocumentContext(DOC_ID);
    documentContext.applyExternalChange(createDocumentElement(
      "3",
      1,
      'A'.charCodeAt(0),
      false,
      "1"
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "1",
      1,
      'B'.charCodeAt(0),
      false,
      undefined
    ));
    documentContext.applyExternalChange(createDocumentElement(
      "2",
      1,
      'C'.charCodeAt(0),
      true,
      "1"
    ));
    expect(documentContext.getDocumentContent()).toEqual("ABC");
});