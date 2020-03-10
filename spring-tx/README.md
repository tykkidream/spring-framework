Spring 事务代码分析
=================

XML 配置实现的代码分析路径：

1. [TxNamespaceHandler](src/main/java/org/springframework/transaction/config/TxNamespaceHandler.java)
1. [AnnotationDrivenBeanDefinitionParser](src/main/java/org/springframework/transaction/config/AnnotationDrivenBeanDefinitionParser.java)

事务拦截的代码分析路径:

1. [TransactionInterceptor](src/main/java/org/springframework/transaction/interceptor/TransactionInterceptor.java)
1. [TransactionAspectSupport](src/main/java/org/springframework/transaction/interceptor/TransactionAspectSupport.java)
1. [AbstractPlatformTransactionManager](src/main/java/org/springframework/transaction/support/AbstractPlatformTransactionManager.java)