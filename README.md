# CompositionAvatar [![](https://jitpack.io/v/YiiGuxing/CompositionAvatar.svg)](https://jitpack.io/#YiiGuxing/CompositionAvatar)
**Android composition avatar**

仿QQ讨论组头像

- 基础
  
  ![基础](./images/base.png)

- 动态增减图像
  
  ![动态](./images/dynamic_drawables.gif)

- 动态设置间距
  
  ![动态](./images/dynamic_gap.gif)

- 动画图像
  
  ![动画](./images/animation.gif)

- Drawable状态（如点击状态）(selector)
  
  ![状态](./images/state.gif)

- 矢量图
  
  ![状态](./images/vector.png)

### 使用
1. 添加 JitPack 仓库地址到项目构建文件 - [`build.gradle`](./build.gradle):
   ```groovy
   allprojects {
     repositories {
       ...
       maven { url 'https://jitpack.io' }
     }
   }
   ```
2. 添加依赖:
   ```groovy
   dependencies {
     compile 'com.github.YiiGuxing:CompositionAvatar:v1.0.1'
   }
   ```
3. 在 `xml` 上使用:
   ```xml
   <cn.yiiguxing.compositionavatar.CompositionAvatarView
       xmlns:app="http://schemas.android.com/apk/res-auto"
       android:layout_width="100dp"
       android:layout_height="wrap_content"
       app:gap="0.25"/>
       <!-- 默认gap为0.25 -->
   ```
   
   详细请看 [`sample`](./sample)
