/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react;

import com.facebook.react.bridge.JSIModulesProvider;
import javax.annotation.Nullable;

import java.util.List;

import android.app.Application;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.JSIModulesProvider;
import com.facebook.react.bridge.JavaScriptExecutorFactory;
import com.facebook.react.bridge.ReactMarker;
import com.facebook.react.bridge.ReactMarkerConstants;
import com.facebook.react.common.LifecycleState;
import com.facebook.react.devsupport.RedBoxHandler;
import com.facebook.react.uimanager.UIImplementationProvider;

/**
 * Simple class that holds an instance of {@link ReactInstanceManager}. This can be used in your
 * {@link Application class} (see {@link ReactApplication}), or as a static field.
 */
//App层持有 和 创建ReactNativeHost 通常一个App只有一个ReactNativeHost(RN主机)
// ReactNativeHost内部持有:ReactInstanceManager管理

// ReactInstanceManager内部持有:ReactContext 和 ReactRootView 负责管理ReactRootView的生命周期,JS文件等的基础配置工作,负责ReactContext和CatalystInstance的构建
// 持有List<ViewManager>

// ReactContext内部持有:CatalystInstance,UI JS Native代码执行的工作队列 负责包装Application或者Activity构建成RN执行的上下文环境,
//  透传Runnable等进入相应的执行队列进行执行

// CatalystInstance实现为CatalystInstanceImpl: 持有:NativeModuleRegistry(java模块) JavaScriptModuleRegistry(JS模块)
//  负责协调JS <---> C++(JS执行引擎) <---> JAVA 三方执行的交互

//  NativeModuleRegistry:JAVA提供给JS调用的模块的持有者
//  JavaScriptModuleRegistry:JS提供给JAVA调用的模块的持有者,实际JAVA层是通过动态代理放射进行JAVA层向JS层的调用
//  ViewManager:各种自定义View的管理类,负责JSView到native View的映射

//  ReactRootView视图渲染流程:
//  ReactActivity(指定Component Name 创建ReactRootView,加载进入自己的ContentView 初始化基础设施)=>ReactRootView#defaultJSEntryPoint调用进入JS查找指定的Component并启动JS代码=>

//  CoreModulesPackage:
// 则为ReactNativeModule的核心组件

//  UIManagerModule:JS和NativeView交互的核心类.其由CoreModulesPackage负责提供,但是不持有List<ViewManager>转而交由ReactInstanceManager持有,同时也负责创建
//  UIImplementation,将List<ViewManager>间接的交由UIImplementation持有,UIManagerModule持有UIImplementation,同时将JS调用的和View相关的工作委托给Implementation进行实现.



// todo: 理论上是否可以通过改写ReactActivityDelegate使其可以应用在单个Fragment中,但是也无法做到一个Activity中的多个Fragment同时使用ReactNative进行实现
// todo: 因为从目前ReactInstance的源码看同时只支持一个正在运行的ReactActivity实例 但是可以考虑将ReactInstanceManager的生命周期与Fragment无关,而直接与容纳Fragment的Activity相关
//  todo: 可以支持一个Activity由 多RN渲染的Fragment构成?
// todo: 参考:https://github.com/hudl/react-native-android-fragment 尝试实现一个Activity多Fragment的实例
public abstract class ReactNativeHost {

  private final Application mApplication;
  private @Nullable ReactInstanceManager mReactInstanceManager;

  protected ReactNativeHost(Application application) {
    mApplication = application;
  }

  /**
   * Get the current {@link ReactInstanceManager} instance, or create one.
   */
  public ReactInstanceManager getReactInstanceManager() {
    if (mReactInstanceManager == null) {
      ReactMarker.logMarker(ReactMarkerConstants.GET_REACT_INSTANCE_MANAGER_START);
      mReactInstanceManager = createReactInstanceManager();
      ReactMarker.logMarker(ReactMarkerConstants.GET_REACT_INSTANCE_MANAGER_END);
    }
    return mReactInstanceManager;
  }

  /**
   * Get whether this holder contains a {@link ReactInstanceManager} instance, or not. I.e. if
   * {@link #getReactInstanceManager()} has been called at least once since this object was created
   * or {@link #clear()} was called.
   */
  public boolean hasInstance() {
    return mReactInstanceManager != null;
  }

  /**
   * Destroy the current instance and release the internal reference to it, allowing it to be GCed.
   */
  public void clear() {
    if (mReactInstanceManager != null) {
      mReactInstanceManager.destroy();
      mReactInstanceManager = null;
    }
  }

  protected ReactInstanceManager createReactInstanceManager() {
    ReactMarker.logMarker(ReactMarkerConstants.BUILD_REACT_INSTANCE_MANAGER_START);
    ReactInstanceManagerBuilder builder = ReactInstanceManager.builder()
      .setApplication(mApplication)
      .setJSMainModulePath(getJSMainModuleName())
      .setUseDeveloperSupport(getUseDeveloperSupport())
      .setRedBoxHandler(getRedBoxHandler())
      .setJavaScriptExecutorFactory(getJavaScriptExecutorFactory())
      .setUIImplementationProvider(getUIImplementationProvider())
      .setJSIModulesProvider(getJSIModulesProvider())
      .setInitialLifecycleState(LifecycleState.BEFORE_CREATE);

    for (ReactPackage reactPackage : getPackages()) {
      builder.addPackage(reactPackage);
    }

    String jsBundleFile = getJSBundleFile();
    if (jsBundleFile != null) {
      builder.setJSBundleFile(jsBundleFile);
    } else {
      builder.setBundleAssetName(Assertions.assertNotNull(getBundleAssetName()));
    }
    ReactInstanceManager reactInstanceManager = builder.build();
    ReactMarker.logMarker(ReactMarkerConstants.BUILD_REACT_INSTANCE_MANAGER_END);
    return reactInstanceManager;
  }

  /**
   * Get the {@link RedBoxHandler} to send RedBox-related callbacks to.
   */
  protected @Nullable RedBoxHandler getRedBoxHandler() {
    return null;
  }

  /**
   * Get the {@link JavaScriptExecutorFactory}.  Override this to use a custom
   * Executor.
   */
  protected @Nullable JavaScriptExecutorFactory getJavaScriptExecutorFactory() {
    return null;
  }

  protected final Application getApplication() {
    return mApplication;
  }

  /**
   * Get the {@link UIImplementationProvider} to use. Override this method if you want to use a
   * custom UI implementation.
   *
   * Note: this is very advanced functionality, in 99% of cases you don't need to override this.
   */
  protected UIImplementationProvider getUIImplementationProvider() {
    return new UIImplementationProvider();
  }

  protected @Nullable
  JSIModulesProvider getJSIModulesProvider() {
    return null;
  }

  /**
   * Returns the name of the main module. Determines the URL used to fetch the JS bundle
   * from the packager server. It is only used when dev support is enabled.
   * This is the first file to be executed once the {@link ReactInstanceManager} is created.
   * e.g. "index.android"
   */
  protected String getJSMainModuleName() {
    return "index.android";
  }

  /**
   * Returns a custom path of the bundle file. This is used in cases the bundle should be loaded
   * from a custom path. By default it is loaded from Android assets, from a path specified
   * by {@link getBundleAssetName}.
   * e.g. "file://sdcard/myapp_cache/index.android.bundle"
   */
  protected @Nullable String getJSBundleFile() {
    return null;
  }

  /**
   * Returns the name of the bundle in assets. If this is null, and no file path is specified for
   * the bundle, the app will only work with {@code getUseDeveloperSupport} enabled and will
   * always try to load the JS bundle from the packager server.
   * e.g. "index.android.bundle"
   */
  protected @Nullable String getBundleAssetName() {
    return "index.android.bundle";
  }

  /**
   * Returns whether dev mode should be enabled. This enables e.g. the dev menu.
   */
  public abstract boolean getUseDeveloperSupport();

  /**
   * Returns a list of {@link ReactPackage} used by the app.
   * You'll most likely want to return at least the {@code MainReactPackage}.
   * If your app uses additional views or modules besides the default ones,
   * you'll want to include more packages here.
   */
  protected abstract List<ReactPackage> getPackages();
}
