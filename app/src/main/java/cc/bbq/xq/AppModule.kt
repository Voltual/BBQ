package cc.bbq.xq

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * 应用程序的主 Koin 模块，使用基于注解的配置。
 *
 * 该模块配置了：
 * - [@Module] - 将该类标记为 Koin 模块
 * - [@ComponentScan] - 自动发现并注册 "cc.bbq.xq" 包中所有带有
 *   Koin 注解（@Single、@Factory、@KoinViewModel）的类
 * - [@Configuration] - 启用高级配置功能和自动模块发现
 *
 * 与 Application 类上的 [@KoinApplication] 结合使用时，模块将自动加载，
 * 无需手动调用 `modules(AppModule().module)`。
 */
@Module
@ComponentScan("cc.bbq.xq")
@Configuration
class AppModule