# cas-site
基于CAS v5.0.4基础上修改

工程使用的是jdk1.8,这点需要注意一下

cas的配置参考：https://apereo.github.io/cas/5.0.x/installation/Configuration-Properties.html#warning-cookie

application.properties
# 自定义踢人功能开关
# 支持相同APP登录相同用户，后者会踢掉前者等登录状态，前提是SLO需要先开启
cas.forceLogout = false
