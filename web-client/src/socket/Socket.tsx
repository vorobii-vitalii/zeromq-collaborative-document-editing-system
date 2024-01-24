
export enum SocketState {
  CLOSED,
  CONNECTING,
  OPEN
}

export interface Socket {
  send:(buffer: Uint8Array) => void;
  state: () => SocketState
}