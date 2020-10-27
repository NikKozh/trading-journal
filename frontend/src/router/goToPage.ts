import Routes from "./Routes"
import router from "./router"

export default {
    contractList: () => router.push({ path: Routes.index }),
    createContract: () => router.push({ path: Routes.createContract }),
    stats: () => router.push({ path: Routes.stats }),
    contractCard: (id: string) => router.push({ path: `${Routes.contractDetails}/${id}/view` }),
    contractForm: (id: string) => router.push({ path: `${Routes.contractDetails}/${id}/edit` }),
    prefillContract: () => router.push({ path: Routes.prefillContract })
}