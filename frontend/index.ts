import "reflect-metadata"
import Vue from "vue"
import ElementUI from "element-ui"
import locale from 'element-ui/lib/locale/lang/ru-RU'
import 'element-ui/lib/theme-chalk/index.css'
import App from "./src/App.vue"
import VueRouter from 'vue-router'
import router from "./src/router/router"
import VueCookies from "vue-cookies-ts"

Vue.use(VueRouter)
Vue.use(ElementUI, { locale })
Vue.use(VueCookies)

new Vue({
    router,
    render: h => h(App)
}).$mount("#app")