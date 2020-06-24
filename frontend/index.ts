import "reflect-metadata"
import Vue from "vue"
import ElementUI from "element-ui"
import 'element-ui/lib/theme-chalk/index.css'
import App from "./src/App.vue"
import VueRouter from 'vue-router'
import router from "./src/router/router"

Vue.use(VueRouter)
Vue.use(ElementUI)

new Vue({
    router,
    render: h => h(App)
}).$mount("#app")