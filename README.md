# 依赖注入容器实战 
[![Java CI with Gradle](https://github.com/maplestoryJin/DiContainer/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/maplestoryJin/DiContainer/actions/workflows/gradle.yml)

## 任务列表
* ~~无需构造的组件——组件实例~~
* ~~如果注册的组件不可实例化，则抛出异常~~
  * 抽象类
  * 接口
* ~~构造函数注入~~
  * ~~无依赖的组件应该通过默认构造函数生成组件实例~~
  * ~~有依赖的组件，通过 Inject 标注的构造函数生成组件实例~~
  * ~~如果所依赖的组件也存在依赖，那么需要对所依赖的组件也完成依赖注入~~
  * ~~如果组件有多于一个 Inject 标注的构造函数，则抛出异常~~
  * ~~如果组件没有 Inject 标注的构造函数，也没有默认构造函数（新增任务），则抛出异常~~
  * ~~如果组件需要的依赖不存在，则抛出异常~~
  * ~~如果组件间存在循环依赖，则抛出异常~~
* ~~字段注入~~
  * ~~通过 Inject 标注将字段声明为依赖组件~~
  * ~~如果字段为 final 则抛出异常~~
  * ~~依赖中应包含 Inject Field 声明的依赖~~
  * ~~如果组件需要的依赖不存在，则抛出异常~~
  * ~~如果组件间存在循环依赖，则抛出异常~~
* ~~方法注入~~
  * ~~通过 Inject 标注的方法，其参数为依赖组件~~
  * ~~通过 Inject 标注的无参数方法，会被调用~~
  * ~~按照子类中的规则，覆盖父类中的 Inject 方法~~
  * ~~如果方法定义类型参数，则抛出异常~~
  * ~~依赖中应包含 Inject Method 声明的依赖~~
* 对 Provider 类型的依赖
  * ~~从容器中取得组件的 Provider（新增任务）~~
  * ~~注入构造函数中可以声明对于 Provider 的依赖~~
  * ~~注入字段中可以声明对于 Provider 的依赖~~
  * ~~注入方法中可声明对于 Provider 的依赖~~
* ~~自定义 Qualifier 的依赖~~
  * ~~注册组件时，可额外指定 Qualifier~~
    * ~~针对 instance 指定一个 Qualifier（新增任务）~~
    * ~~针对组件指定一个 Qualifier （新增任务）~~
    * ~~针对 instance 指定多个 Qualifier （新增任务）~~
    * ~~针对组件指定多个Qualifier （新增任务）~~
  * ~~注册组件时，如果不是合法的Qualifier，则不接收组件注册（新增任务）~~
  * ~~寻找依赖时，需同时满足类型与自定义 Qualifier 标注~~
    * ~~在检查依赖时使用 Qualifier（新增任务）~~
    * ~~在检查循环依赖时使用 Qualifier （新增任务）~~
    * ~~构造函数注入可以使用 Qualifier 声明依赖（新增任务）~~
      * ~~如果不是合法的 Qualifier ，则组件非法~~
    * ~~字段注入可以使用 Qualifier 声明依赖（新增任务）~~
      * ~~如果不是合法的 Qualifier ，则组件非法~~
    * ~~函数注入可以使用 Qualifier 声明依赖（新增任务）~~
      * ~~如果不是合法的 Qualifier ，则组件非法~~
  * ~~支持默认 Qualifier——Named（不需要）~~
  * ~~注册组件时，可从类对象上提取 Qualifier（不需要）~~
* ~~Singleton 生命周期~~
  * ~~注册组件时，可额外指定是否为 Singleton~~
  * ~~注册组件时，可从类对象上提取 Singleton 标注~~
  * ~~对于包含 Singleton 标注的组件，在容器范围内提供唯一实~~
  * ~~例容器组件默认不是 Single 生命周期~~
* ~~自定义 Scope 标注~~
  * ~~可向容器注册自定义 Scope 标注的回调~~

