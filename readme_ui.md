# ð¦ Packge: <span style="color: purple">nxp</span>
flutter\nxp_nfcApp\nxp

## ç·¨è¼¯èªç³»å§å®¹
ç·¨è¼¯ä¸­è±æåæ,éè®æ´ä»¥ä¸æªæ¡
#### â éé£æªæ¡
  - ð [nxp/lib/consts/messages.dart](lib/consts/messages.dart)
  - ð [nxp/lib/pages/language.dart](lib/pages/language.dart)
#### 1.è®æ´éæUIä¹ä¸­è±æå - ð [nxp/lib/consts/messages.dart](lib/consts/messages.dart)
![](.readme_ui_images/language_ui.jpeg)
- è¨­å®æ¹å¼
    - æ¼UIé¡å¥ä¸, ææä¿¡æ¯åä»¥indexæ¹æ³åè£¹ä½,   
      åçºä¸åæ¬ä½, 0çºè±æ, 1çºç¹ä¸­, 2çºç°¡, å¦ä¸å

#### 2.è®æ´Appé¡¯ç¤ºçåæè¨æ¯(å¦å¯«å¥æå/å¤±æ) - ð [nxp/lib/consts/messages.dart](lib/consts/messages.dart)

![](.readme_ui_images/language_msg.jpeg)
- è¨­å®æ¹å¼
    - æ¼Msgé¡å¥ä¸, æææ¯åä»¥indexæ¹æ³åè£¹ä½,   
      åçºä¸åæ¬ä½, 0çºè±æ, 1çºç¹ä¸­, 2çºç°¡, å¦ä¸å


## ç·¨è¼¯UI
### ð Account Pages
 - ð [signup](lib/pages/userRegister.dart)
 - ð [login](lib/pages/userLogin.dart)
 - ð [authorized](lib/pages/userAuthorized.dart)
#### 1. Signup - ð [userRegister.dart](lib/pages/user/userRegister.dart)

![](.readme_ui_images/user_signup.jpeg)

![](.readme_ui_images/user_filestruct.jpeg)

- __å°æç¢¼:__
    - __email field__: [lib/pages/user/userRegister:buildAccountNameForm](./lib/pages/user/userRegister.dart)
    - __nickname_field__: [lib/pages/user/userRegister:buildNicknameForm](./lib/pages/user/userRegister.dart)
    - __password_field__: [lib/pages/user/userRegister:buildPasswordForm](./lib/pages/user/userRegister.dart)
    - __permission_field__: [lib/pages/user/userRegister:buildPermissionRadios](./lib/pages/user/userRegister.dart)
    - ![](.readme_ui_images/userRegister_code1.jpeg)

    - __signup__: [lib/pages/user/userRegister:signup](./lib/pages/user/userRegister.dart)
    - ![](.readme_ui_images/userRegiser_signup.jpeg

#### 2.logout - ð [userAuthorized.dart](lib/pages/user/userAuthorized.dart)

![](.readme_ui_images/userAuthorized_logout.jpeg)

- __å°æç¢¼:__
    - __logout__: [lib/pages/user/userRegister:buildAccountNameForm](./lib/pages/user/userRegister.dart)



### ð main menu
 - ð [zoom_scaffold_menu](lib/pages/zoom_scaffold_menu.dart)
#### nxp/lib/pages/zoom_scaffold_menu.dart
![](.readme_ui_images/mainmenu.jpeg)

![](.readme_ui_images/mainmenu_code1.jpeg)  
åé¸å®å°æä¹ç¨å¼ç¢¼å¦ä¸å


### ð Settings
#### nxp/lib/pages/settings.dart

![](.readme_ui_images/settings.jpeg)

- __å°æç¢¼:__
    - __File Server__: [lib/pages/user/settings:buildLocalServerSwitch](./lib/pages/user/settings.dart)
    - __Language__: [lib/pages/user/settings:buildLanguage](./lib/pages/user/settings.dart)
    - __AppStorage__: [lib/pages/user/settings:buildCustomConfigPath](./lib/pages/user/settings.dart)
    - __NFCSound__: [lib/pages/user/settings:buildNFCSound](./lib/pages/user/settings.dart)
    - __NFC IO Setting__: [lib/pages/user/settings:buildCycleIOSettings](./lib/pages/user/settings.dart)
    - __NFC IO mock__: [lib/pages/user/settings:buildCycleIOMock](./lib/pages/user/settings.dart)
    - ![](.readme_ui_images/settings_code.png)

- NFC read/write IO setting  
![](.readme_ui_images/settings_nfcio.png)  
è¨­å®éè¨ééåæå¤§éè©¦æ¬¡æ¸

- NFC IO mock  
![](.readme_ui_images/setting_nfcio_mock.jpeg)  
æ¨¡æ¬éè¨æ¼ç¬¬ __N__ æ¬¡å¯«å¥ __æå__ / __å¤±æ__,åç¨æ¨¡æ¬åè½æ <span style="color:orange">å¿½ç¥</span>  
å¯¦ééè¨ææè®åçå¼, åæ¶æäºç¨®æ¹å¼  
    - user é»é¸åæ¶enable 
    - å¨UIä¸ç´æ¥åæ¶MOCKåè½, ä¿®æ¹ buildCycleIOMock
    - ![](.readme_ui_images/setting_cycleiomock_code.jpeg)  
        buildCycleIOMock({enable = <span style="color:orange">true</span> æ¹çº   
        buildCycleIOMock({enable = <span style="color:red">false</span>    
