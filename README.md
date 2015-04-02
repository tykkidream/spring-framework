## Spring Framework 源码分析
Spring的版本为3.2.8。

分析时使用了《Spring源码深度解析》的说明，网上一些Blog中的分析，以及少部分的个人理解。

目前分析的主要内容是Spring的IOC容器，spring-core和spring-bean。分析以在重要的代码处添加
//中文解释。类说明、方法说明、字段说明一般在原英文注释前添加段落，并以横线&lt;hr&gt;作为分
隔。原英文注释后的中文是用Google翻译的，会有一些不通顺。

如果想阅读中文注释，最好是将源码在eclipse中，使用javadoc视图阅读（将鼠标移动到类名、方法
等上时会自动弹出浮动窗口），而且能快速地跳转到相关代码处（Ctrl+鼠标左键点击）。

## Spring Framework
The Spring Framework provides a comprehensive programming and configuration
model for modern Java-based enterprise applications - on any kind of deployment
platform. A key element of Spring is infrastructural support at the application
level: Spring focuses on the "plumbing" of enterprise applications so that teams
can focus on application-level business logic, without unnecessary ties to
specific deployment environments.

Spring框架为当前在任何类型的平台上部署基于Java的企业应用程序提供了全面的编程和配置模型。Spring的一个关键内容是对应用层的基础支持：

The framework also serves as the foundation for [Spring Integration][], [Spring
Batch][] and the rest of the Spring [family of projects][]. Browse the
repositories under the [SpringSource organization][] on GitHub for a full list.

[.NET][] and [Python][] variants are available as well.

## Downloading artifacts
See [downloading Spring artifacts][] for Maven repository information. Unable to
use Maven or other transitive dependency management tools? See [building a
distribution with dependencies][].

## Documentation
See the current [Javadoc][] and [reference docs][].

## Getting support
Check out the [Spring forums][] and the [spring][spring tag] and
[spring-mvc][spring-mvc tag] tags on [Stack Overflow][]. [Commercial support][]
is available too.

## Issue Tracking
Report issues via the [Spring Framework JIRA]. Understand our issue management
process by reading about [the lifecycle of an issue][]. Think you've found a
bug? Please consider submitting a reproduction project via the
[spring-framework-issues][] GitHub repository. The [readme][] there provides
simple step-by-step instructions.

## Building from source
The Spring Framework uses a [Gradle][]-based build system. In the instructions
below, [`./gradlew`][] is invoked from the root of the source tree and serves as
a cross-platform, self-contained bootstrap mechanism for the build. The only
prerequisites are [Git][] and JDK 1.7+.

### check out sources
`git clone git://github.com/SpringSource/spring-framework.git`

### compile and test, build all jars, distribution zips and docs
`./gradlew build`

### install all spring-\* jars into your local Maven cache
`./gradlew install`

### import sources into your IDE
Run `./import-into-eclipse.sh` or read `import-into-idea.md` as appropriate.

... and discover more commands with `./gradlew tasks`. See also the [Gradle
build and release FAQ][].

## Contributing
[Pull requests][] are welcome; see the [contributor guidelines][] for details.

## Staying in touch
Follow [@springframework][] and its [team members][] on Twitter. In-depth
articles can be found at the SpringSource [team blog][], and releases are
announced via our [news feed][].

## License
The Spring Framework is released under version 2.0 of the [Apache License][].

[Spring Integration]: https://github.com/SpringSource/spring-integration
[Spring Batch]: https://github.com/SpringSource/spring-batch
[family of projects]: http://springsource.org/projects
[SpringSource organization]: https://github.com/SpringSource
[.NET]: https://github.com/SpringSource/spring-net
[Python]: https://github.com/SpringSource/spring-python
[downloading Spring artifacts]: https://github.com/SpringSource/spring-framework/wiki/Downloading-Spring-artifacts
[building a distribution with dependencies]: https://github.com/SpringSource/spring-framework/wiki/Building-a-distribution-with-dependencies
[Javadoc]: http://static.springsource.org/spring-framework/docs/current/javadoc-api
[reference docs]: http://static.springsource.org/spring-framework/docs/current/spring-framework-reference
[Spring forums]: http://forum.springsource.org
[spring tag]: http://stackoverflow.com/questions/tagged/spring
[spring-mvc tag]: http://stackoverflow.com/questions/tagged/spring-mvc
[Stack Overflow]: http://stackoverflow.com/faq
[Commercial support]: http://springsource.com/support/springsupport
[Spring Framework JIRA]: http://jira.springsource.org/browse/SPR
[the lifecycle of an issue]: https://github.com/cbeams/spring-framework/wiki/The-Lifecycle-of-an-Issue
[spring-framework-issues]: https://github.com/SpringSource/spring-framework-issues#readme
[readme]: https://github.com/SpringSource/spring-framework-issues#readme
[Gradle]: http://gradle.org
[`./gradlew`]: http://vimeo.com/34436402
[Git]: http://help.github.com/set-up-git-redirect
[Gradle build and release FAQ]: https://github.com/SpringSource/spring-framework/wiki/Gradle-build-and-release-FAQ
[Pull requests]: http://help.github.com/send-pull-requests
[contributor guidelines]: https://github.com/SpringSource/spring-framework/blob/master/CONTRIBUTING.md
[@springframework]: http://twitter.com/springframework
[team members]: http://twitter.com/springframework/team/members
[team blog]: http://blog.springsource.org
[news feed]: http://www.springsource.org/news-events
[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
