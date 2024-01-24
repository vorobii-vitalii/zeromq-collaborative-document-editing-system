import React, { useState } from 'react';
import { WelcomeScreen } from "./views/WelcomeScreen";
import { EditDocumentView } from "./views/EditDocumentView";
import { WSSocketFactory } from "./socket/SocketFactory";

const socketFactory = new WSSocketFactory();
const uuidGenerator = () => crypto.randomUUID();

export const App = () => {
  const [initialized, setInitialized] = useState(false);
  const [documentId, setDocumentId] = useState<number>(0);
  const [userId, setUserId] = useState<number>(0);

  if (!initialized) {
    return (
      <WelcomeScreen onFormSubmitted={(docId, userId) => {
        setDocumentId(docId);
        setUserId(userId);
        setInitialized(true);
      }} />
    )
  }
  return (
    <EditDocumentView
      userId={userId}
      documentId={documentId}
      socketFactory={socketFactory}
      idGenerator={uuidGenerator}
    />
  );
}
