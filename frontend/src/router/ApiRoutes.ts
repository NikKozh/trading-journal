import Config from '../Config'

function withApiPath(route: string) {
    return Config.appUrl + route
}

export default {
    pingMessage: withApiPath("/ping"),
    signIn: withApiPath("/signIn"),
    // TODO: пока signOut это просто затирание куки, но после написания нормальной авторизации нужен будет роут к API

    contractList: withApiPath("/contractList"),
    contractCard: (id: string) => withApiPath(`/contractDetails/${id}`),
    submitContract: withApiPath("/submitContract"),
    prefillContract: withApiPath("/prefillContract"),
    deleteContract: (id: string) => withApiPath(`/deleteContract/${id}`),
    newContractData: withApiPath("/newContractData"),

    allTimeStats: withApiPath("/stats/allTime"),
    dailyStats: withApiPath("/stats/daily"),
    weeklyStats: withApiPath("/stats/weekly"),
    monthlyStats: withApiPath("/stats/monthly"),
    allStats: withApiPath("/stats/all")
}