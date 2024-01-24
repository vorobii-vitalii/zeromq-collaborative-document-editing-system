import React, { useEffect, useState } from 'react';
import { Alert, Button, Typography } from "antd";
import { SocketFactory } from "../socket/SocketFactory";
import { Socket, SocketState } from "../socket/Socket";
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

const CONNECTION_TIMEOUT = 5000;

export const EditDocumentView = (props: EditDocumentViewProps) => {
  const { userId, documentId, socketFactory, idGenerator } = props;
  const [socket, setSocket] = useState<Socket>();
  const [previousContent, setPreviousContent] = useState("");
  const [documentContext, setDocumentContext] = useState<DocumentContext>();
  const [shouldTimeout, setShouldTimeout] = useState(false);

  useEffect(() => {
    setTimeout(() => {
      // TODO: Check if still not connected
    }, CONNECTION_TIMEOUT)
  }, [socket]);

  useEffect(() => {
    if (socket) {
      return;
    }
    const context = new DocumentContext(documentId);
    // Send connect to server and handle returned messages...
    try {
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
      setSocket(newSocket);
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
    }
    catch (e) {
      console.warn("Error occurred when connecting to server...");
    }
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
  if (shouldTimeout) {
    return (
      <>
        <Alert
          message="Connection timeout"
          description={`Connection couldn't be established in ${CONNECTION_TIMEOUT} ms. `}
          type="error"
        />
        <Button type="primary" block onClick={() => window.location.reload()}>Try again</Button>
      </>
    );
  }
  // if (socket.state() === SocketState.CLOSED) {
  //   return (
  //     <Alert
  //       message="Server error"
  //       description="Connection to server has failed, please retry later..."
  //       type="error"
  //     />
  //   );
  // }
  // if (socket.state() === SocketState.CONNECTING) {
  //   return (
  //     <Alert
  //       message="Connecting"
  //       description="Connecting to server. Hold on..."
  //       type="info"
  //     />
  //   );
  // }
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
