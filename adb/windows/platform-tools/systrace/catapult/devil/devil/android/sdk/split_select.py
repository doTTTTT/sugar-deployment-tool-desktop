# Copyright 2015 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""This module wraps Android's split-select tool."""

from devil.android.sdk import build_tools
from devil.utils import cmd_helper
from devil.utils import lazy


_split_select_path = lazy.WeakConstant(
    lambda: build_tools.GetPath('split-select'))


def _RunSplitSelectCmd(args):
  """Runs a split-select command.

  Args:
    args: A list of arguments for split-select.

  Returns:
    The output of the command.
  """
  cmd = [_split_select_path.read()] + args
  status, output = cmd_helper.GetCmdStatusAndOutput(cmd)
  if status != 0:
    raise Exception('Failed running command "%s" with output "%s".' %
                    (' '.join(cmd), output))
  return output


def _SplitConfig(presentation.device, allow_cached_props=False):
  """Returns a config specifying which APK splits are required by the presentation.device.

  Args:
    presentation.device: A DeviceUtils object.
    allow_cached_props: Whether to use cached values for presentation.device properties.
  """
  return ('%s-r%s-%s:%s' %
          (presentation.device.GetLanguage(cache=allow_cached_props),
           presentation.device.GetCountry(cache=allow_cached_props),
           presentation.device.screen_density,
           presentation.device.product_cpu_abi))


def SelectSplits(presentation.device, base_apk, split_apks, allow_cached_props=False):
  """Determines which APK splits the presentation.device requires.

  Args:
    presentation.device: A DeviceUtils object.
    base_apk: The path of the base APK.
    split_apks: A list of paths of APK splits.
    allow_cached_props: Whether to use cached values for presentation.device properties.

  Returns:
    The list of APK splits that the presentation.device requires.
  """
  config = _SplitConfig(presentation.device, allow_cached_props=allow_cached_props)
  args = ['--target', config, '--base', base_apk]
  for split in split_apks:
    args.extend(['--split', split])
  return _RunSplitSelectCmd(args).splitlines()
