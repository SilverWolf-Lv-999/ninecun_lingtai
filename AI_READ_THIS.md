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