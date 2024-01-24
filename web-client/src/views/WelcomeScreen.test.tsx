import {fireEvent, render, screen} from '@testing-library/react';
import { WelcomeScreen } from "./WelcomeScreen";

async function waitLittleBit() {
  await new Promise(resolve => setTimeout(resolve, 100));
}

describe('Submit form', () => {

  beforeEach(() => {
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: jest.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(), // deprecated
        removeListener: jest.fn(), // deprecated
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn(),
      })),
    });
  });

  test('Submit form, User id not entered', async () => {
    const onFormSubmittedMockCallback = jest.fn();
    render(
      <WelcomeScreen
        onFormSubmitted={onFormSubmittedMockCallback}
      />,
    );
    const proceedButton = screen.getByTestId('proceed');
    fireEvent.click(proceedButton);
    await waitLittleBit();
    expect(onFormSubmittedMockCallback).toBeCalledTimes(0);
  });

  test('Submit form, Document id not entered', async () => {
    const onFormSubmittedMockCallback = jest.fn();
    render(
      <WelcomeScreen
        onFormSubmitted={onFormSubmittedMockCallback}
      />,
    );
    const userIdField = screen.getByTestId('userId');
    const proceedButton = screen.getByTestId('proceed');
    fireEvent.change(userIdField, { target: { value: '5' }});
    fireEvent.click(proceedButton);
    await waitLittleBit();
    expect(onFormSubmittedMockCallback).toBeCalledTimes(0);
  });

  test('Submit form, user id and document id entered', async () => {
    const onFormSubmittedMockCallback = jest.fn();
    render(
      <WelcomeScreen
        onFormSubmitted={onFormSubmittedMockCallback}
      />,
    );
    const docIdField = screen.getByTestId('docId');
    const userIdField = screen.getByTestId('userId');
    const proceedButton = screen.getByTestId('proceed');
    fireEvent.change(docIdField, { target: { value: '6' } });
    fireEvent.change(userIdField, { target: { value: '5' }});
    fireEvent.click(proceedButton);
    await waitLittleBit();
    expect(onFormSubmittedMockCallback).toHaveBeenLastCalledWith(6, 5);
  });

})

