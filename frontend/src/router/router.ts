import Index from "../views/Index.vue"
import CreateContract from "../views/CreateContract.vue"
import Stats from "../views/Stats.vue"
import {VueConstructor} from "vue"
import VueRouter from "vue-router"
import Routes from "./Routes"
import ContractCard from "../views/ContractCard.vue"
import ContractPrefillForm from "../components/ContractPrefillForm.vue"
import SignIn from "../views/SignIn.vue"
import SignOut from "../views/SignOut.vue"

type RouteObject = {
    path: string,
    component: VueConstructor
}

const routes: RouteObject[] = [
    { path: Routes.index, component: Index },
    { path: Routes.createContract, component: CreateContract },
    { path: Routes.prefillContract, component: ContractPrefillForm },
    { path: `${Routes.contractDetails}/:id/:mode`, component: ContractCard },
    { path: Routes.stats, component: Stats },
    { path: Routes.signIn, component: SignIn },
    { path: Routes.signOut, component: SignOut },
    // TODO: { path: '*', component: NotFoundComponent }
]

export default new VueRouter({
    mode: 'history',
    routes
})