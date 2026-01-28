import React from 'react';
import {
  findNodeHandle,
  requireNativeComponent,
  UIManager,
  ViewProps,
} from 'react-native';

export type FaceLivenessEvent = {
  hasFace: boolean;
  confidence: number;
  threshold: number;
  left: number;
  top: number;
  right: number;
  bottom: number;
  timeMs: number;
};

export type FaceLivenessErrorEvent = {
  code: string;
  message: string;
};

export type FaceLivenessViewProps = ViewProps & {
  cameraFacing?: 'front' | 'back';
  threshold?: number;
  analysisIntervalMs?: number;
  active?: boolean;
  onLiveness?: (event: { nativeEvent: FaceLivenessEvent }) => void;
  onError?: (event: { nativeEvent: FaceLivenessErrorEvent }) => void;
};

const COMPONENT_NAME = 'FaceLivenessView';
const NativeFaceLivenessView = requireNativeComponent<FaceLivenessViewProps>(
  COMPONENT_NAME
);

export type FaceLivenessViewRef = React.ElementRef<typeof NativeFaceLivenessView>;

function dispatchCommand(ref: FaceLivenessViewRef | null, command: string) {
  if (!ref) return;
  const node = findNodeHandle(ref);
  if (node == null) return;
  const config = UIManager.getViewManagerConfig(COMPONENT_NAME);
  const commandId = config?.Commands?.[command];
  if (commandId == null) return;
  UIManager.dispatchViewManagerCommand(node, commandId, []);
}

export function start(ref: FaceLivenessViewRef | null) {
  dispatchCommand(ref, 'start');
}

export function stop(ref: FaceLivenessViewRef | null) {
  dispatchCommand(ref, 'stop');
}

export function FaceLivenessView(props: FaceLivenessViewProps) {
  return <NativeFaceLivenessView {...props} />;
}

export default FaceLivenessView;
