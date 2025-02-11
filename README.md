# handson-tomcat
致敬传奇，手写框架之tomcat

与handson-spring一样，handson-tomcat依旧采取多分支开发的方式，分支结构如下：

- a-server01：实现一个最简单的静态资源服务器
- a-server02：服务器引入commons-lang3，自定义动态资源Servlet，并支持Servlet内容动态填充
- a-server03：服务器引入javax.servlet-api，适配标准的Servlet规范
- b-connector01：引入Tomcat连接层，同时对HttpServer一拆为二Connector和Processor，做到职责分离
- b-connector02：提高服务器性能，实现Processor对象池
- b-connector03：进一步提高服务器性能，设计线程 Processor，同时实现与Connector线程之间的同步机制
- 更多分支正在更新中...

main分支包含以上所有功能特性

