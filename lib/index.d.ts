import React from 'react';
import { ViewProps } from 'react-native';

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

export declare function FaceLivenessView(
  props: FaceLivenessViewProps
): React.ReactElement | null;

export declare function start(ref: any): void;
export declare function stop(ref: any): void;

export default FaceLivenessView;
