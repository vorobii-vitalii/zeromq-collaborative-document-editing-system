import { ResponseHolder } from "../document/editing/response-holder";
import { Socket, SocketState } from "./Socket";
import { ByteBuffer } from "flatbuffers";

export interface SocketFactory {
  establishConnection: (docId: number, messageHandler: (msg: ResponseHolder) => void) => Socket
}

const WS_URL = "ws://localhost:3102/documents";

export class WSSocketFactory implements SocketFactory {
  establishConnection(docId: number, messageHandler: (msg: ResponseHolder) => void): Socket {
    const socket = new WebSocket(WS_URL + "/" + docId);
    socket.binaryType = "arraybuffer";
    socket.onmessage = message => {
      const bufferSource: ArrayBuffer = message.data;
      const arr = new Uint8Array(bufferSource);
      const responseHolder =
        ResponseHolder.getRootAsResponseHolder(new ByteBuffer(arr));
      console.log(
        `Received message of length = ${arr.length} type = ${responseHolder.responseType()}`);
      messageHandler(responseHolder);
    };

    const waitForConnection = (callback: () => void, interval: number) => {
      if (socket.readyState === 1) {
        callback();
      } else {
        setTimeout(() => waitForConnection(callback, interval), interval);
      }
    }

    return {
      send: (buffer) => {
        console.log(`Sending message of length ${buffer.length}`);
        waitForConnection(() => socket.send(buffer), 100);
      },
      state: () => {
        switch (socket.readyState) {
          case socket.CLOSED:
          case socket.CLOSING:
            return SocketState.CLOSED;
          case socket.CONNECTING:
            return SocketState.CONNECTING;
          default:
            return SocketState.OPEN;
        }
      }
    };
  }
}

