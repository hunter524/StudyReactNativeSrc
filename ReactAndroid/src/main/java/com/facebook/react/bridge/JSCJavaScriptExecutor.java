/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.bridge;

import com.facebook.jni.HybridData;
import com.facebook.proguard.annotations.DoNotStrip;

@DoNotStrip
/* package */ class JSCJavaScriptExecutor extends JavaScriptExecutor {
//  在后台进程初始化ReactContext以及CatalysInstance的时候初始化 c++的引擎
  static {
    ReactBridge.staticInit();
  }

  /* package */ JSCJavaScriptExecutor(ReadableNativeMap jscConfig) {
    super(initHybrid(jscConfig));
  }

  @Override
  public String getName() {
    return "JSCJavaScriptExecutor";
  }


  private native static HybridData initHybrid(ReadableNativeMap jscConfig);
}
