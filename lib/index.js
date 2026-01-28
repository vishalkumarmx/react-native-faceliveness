'use strict';

const React = require('react');
const { findNodeHandle, requireNativeComponent, UIManager } = require('react-native');

const COMPONENT_NAME = 'FaceLivenessView';
const NativeFaceLivenessView = requireNativeComponent(COMPONENT_NAME);

function dispatchCommand(ref, command) {
  if (!ref) return;
  const node = findNodeHandle(ref);
  if (node == null) return;
  const config = UIManager.getViewManagerConfig(COMPONENT_NAME);
  const commandId = config && config.Commands && config.Commands[command];
  if (commandId == null) return;
  UIManager.dispatchViewManagerCommand(node, commandId, []);
}

function start(ref) {
  dispatchCommand(ref, 'start');
}

function stop(ref) {
  dispatchCommand(ref, 'stop');
}

function FaceLivenessView(props) {
  return React.createElement(NativeFaceLivenessView, props);
}

module.exports = FaceLivenessView;
module.exports.FaceLivenessView = FaceLivenessView;
module.exports.start = start;
module.exports.stop = stop;
