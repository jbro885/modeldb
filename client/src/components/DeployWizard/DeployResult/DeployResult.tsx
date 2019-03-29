import * as React from 'react';
import { connect } from 'react-redux';

import Button from 'components/shared/Button/Button';
import CopyToClipboard from 'components/shared/CopyToClipboard/CopyToClipboard';
import Form from 'components/shared/Form/Form';
import Popup from 'components/shared/Popup/Popup';
import { IDeployedStatusInfo } from 'models/Deploy';

import { IApplicationState } from 'store/store';
import styles from './DeployResult.module.css';
import ModelInput from './ModelInput/ModelInput';

interface IProps {
  data: IDeployedStatusInfo['data'];
  onClose(): void;
}

type AllProps = IProps;

class DeployResult extends React.Component<AllProps> {
  public render() {
    const {
      data: { api, modelApi },
      onClose,
    } = this.props;
    return (
      <Popup title="Deployed" isOpen={true} onRequestClose={onClose}>
        <div className={styles.deploy_result}>
          <div className={styles.commonInfo}>
            <Form>
              <Form.Item label="Model ID">22</Form.Item>
              <Form.Item label="Type">REST</Form.Item>
              <Form.Item
                label="URL"
                additionalContent={
                  <Button variant="like-link" fullWidth={true} to={api}>
                    <span className={styles.url}>{api}</span>
                  </Button>
                }
              >
                <CopyToClipboard text={api}>
                  {onCopy => (
                    <Button variant="like-link" onClick={onCopy}>
                      Copy
                    </Button>
                  )}
                </CopyToClipboard>
              </Form.Item>
            </Form>
          </div>
          <div className={styles.model_input}>
            <ModelInput input={modelApi.input} />
          </div>
        </div>
      </Popup>
    );
  }
}

export default DeployResult;
