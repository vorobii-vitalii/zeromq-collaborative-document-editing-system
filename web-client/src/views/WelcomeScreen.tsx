import React from 'react';
import { Button, Form, Input, Typography } from 'antd';

const DOCUMENT_ID = "documentId";
const USER_ID = "userId";

interface WelcomeScreenProps {
  onFormSubmitted: (documentId: number, userId: number) => void;
}

export const WelcomeScreen = (props : WelcomeScreenProps) => {
  const [form] = Form.useForm();

  const onFormSubmit = async (e: any) => {
    try {
      const values = await form.validateFields({
      });
      const documentId = Number.parseInt(values[DOCUMENT_ID]);
      const userId = Number.parseInt(values[USER_ID]);
      if (documentId && userId) {
        console.log(`Form submitted with documentId = ${documentId} userId = ${userId}`);
        props.onFormSubmitted(documentId, userId);
      }
    }
    catch (e) {
      console.log("Validation failed.")
    }
  };

  return (
    <>
      <Typography.Title>
        Documents application, please enter which document you want to edit and your user id!
      </Typography.Title>
      <Form
        layout={"vertical"}
        form={form}
        onFinish={onFormSubmit}
      >
        <Form.Item required={true}
                   name={DOCUMENT_ID}
                   rules={[
                     {
                       required: true, message: 'Please input documentId'
                     }
                   ]}
                   label="Enter which document you wanna edit">
          <Input placeholder="1" data-testid="docId" />
        </Form.Item>
        <Form.Item required={true}
                   name={USER_ID}
                   rules={[
                     {
                       required: true, message: 'Please input user id'
                     }
                   ]}
                   label="Enter your user id">
          <Input placeholder="123" data-testid="userId" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={onFormSubmit} data-testid="proceed" >
            Proceed
          </Button>
        </Form.Item>
      </Form>
    </>
  );
};
