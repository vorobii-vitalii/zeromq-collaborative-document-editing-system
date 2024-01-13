import React, { useEffect, useState } from 'react';
import { Typography } from "antd";
import { SocketFactory } from "../socket/SocketFactory";
import { Socket } from "../socket/Socket";
import { Response } from "../document/editing/response";
import { DocumentElement } from "../document/editing/document-element";
import { DocumentContext } from "../tree_doc/DocumentContext";
import TextArea from "antd/es/input/TextArea";
import { GetRequest, Request, RequestHolder } from "../document/editing";
import { Builder } from "flatbuffers";

interface EditDocumentViewProps {
  userId: number;
  documentId: number;
  socketFactory: SocketFactory;
  idGenerator: () => string;
}

export const EditDocumentView = (props: EditDocumentViewProps) => {
  const { userId, documentId, socketFactory, idGenerator } = props;
  const [socket, setSocket] = useState<Socket>();
  const [previousContent, setPreviousContent] = useState("");
  const [documentContext, setDocumentContext] = useState<DocumentContext>();

  useEffect(() => {
    if (socket) {
      return;
    }
    const context = new DocumentContext(documentId);
    // Send connect to server and handle returned messages...
    let newSocket = socketFactory.establishConnection(documentId, msg => {
      const responseType = msg.responseType();
      if (responseType === Response.DocumentElement) {
        console.log("Adding new element to context");
        const docElement : DocumentElement = msg.response(new DocumentElement());
        context.applyExternalChange(docElement);
        setPreviousContent(context.getDocumentContent());
      }
      else if (responseType === Response.ChangeResponse) {
        console.log("Change was committed!");
      }
      else {
        console.warn(`Cannot handle response of type = ${responseType}`);
      }
    });
    setDocumentContext(context);
    console.log(`Fetching document ${documentId}`)
    const builder = new Builder(0);
    GetRequest.startGetRequest(builder);
    GetRequest.addDocumentId(builder, documentId);
    const getRequestOffset = GetRequest.endGetRequest(builder);
    RequestHolder.startRequestHolder(builder);
    RequestHolder.addRequestType(builder, Request.GetRequest);
    RequestHolder.addRequest(builder, getRequestOffset);
    const requestHolder = RequestHolder.endRequestHolder(builder);
    builder.finish(requestHolder);
    newSocket.send(builder.asUint8Array());
    setSocket(newSocket);
  }, [documentContext, documentId, socket, socketFactory]);

  const onUserDocumentChange = (e: { target: { value: any; }; }) => {
    const updatedDocumentContent = e.target.value;
    const changesToApply =
      documentContext!!.applyUserChange(updatedDocumentContent, userId, idGenerator);
    setPreviousContent(updatedDocumentContent);
    socket?.send(changesToApply);
  };

  if (!socket) {
    return null;
  }
  return (
    <>
      <Typography.Title>Welcome {userId}! You are editing document {documentId}!</Typography.Title>
      <TextArea
        value={previousContent}
        onChange={onUserDocumentChange}
        data-testid="documentTextArea"
        cols={140}
        rows={50}
      />
    </>
  );
}
