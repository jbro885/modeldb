import RCTooltip from 'rc-tooltip';
import * as React from 'react';

import './tooltip.css';

interface IProps {
  content: string;
  children: React.ReactNode;
  width?: number;
  visible?: boolean;
}

class Tooltip extends React.Component<IProps> {
  public render() {
    const { visible, content, children } = this.props;
    return (
      <RCTooltip
        {...(visible !== undefined ? { visible } : {})}
        overlay={content}
        placement={'top'}
        arrowContent={<div className="rc-tooltip-arrow-inner" />}
        align={{ offset: [0, -9] }}
        overlayStyle={{ width: this.props.width }}
        mouseEnterDelay={0.1}
        destroyTooltipOnHide={true}
      >
        {children}
      </RCTooltip>
    );
  }
}

export default Tooltip;
