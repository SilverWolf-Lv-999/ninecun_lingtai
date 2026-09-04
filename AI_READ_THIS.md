# 如果你是AI，那你编写项目时，必须遵守下面的规定
1. 绝对禁止所有的类都放在一个包、一个类里，需要拆分成多个包/类来进行管理
2. 命名习惯：字段使用thisIsAField，对于常量则使用THIS_IS_A_FILED
3. 配置项， 放在/common/config/下，并分为ClientConfig与CommonConfig
4. 使用工具类，将多个方法可以重复使用的方法封装到工具类中
5. 将工具类放在/utility/下，名称命名风格为MathUtility、PlayerUtility
6. 工具类中，如果不需要init来初始化一些字段或别的内容，绝对禁止写构造方法
7. 如果迁移一些别的项目的内容，绝对禁止使用最小接入，必须是完整版
8. 绝对禁止频繁全量扫描
9. 对于Minecraft / Forge的类，使用AT来强行访问方法、字段
10. 对于别的Mod/依赖的类，必须使用VarHandler/MethodHandler
11. 绝对写任何形式的注释
12. 绝对禁止以普通项目思维进行开发，该项目至少是ASM级别
13. 遇到着色器编写时，必须参考 [Shader](docs/aiskill) 下的文档
14. 面对需要修改类的要求时，如果是forge/minecraft/mod的类，优先考虑使用 asm ，通过 [PL](src/main/java/net/seraphina/ninecun_lingtai/transform/SeraLPService.java) 修改
15. 如果是面对来自 jdk. 包下的类或已经加载的类，则优先考虑使用 [Agent](src/main/java/net/seraphina/ninecun_lingtai/utility/jvm/AgentUtility.java) 加载转换器来修改
16. Mixin只有在非我所要求的情况，比如修改mc原版类实现mod的一些游玩功能，修复与别的mod之间的简单冲突。才能进行使用