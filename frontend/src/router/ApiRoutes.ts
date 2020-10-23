import Config from '../Config'

function withApiPath(route: string) {
    return Config.appUrl + route
}

export default {
    pingMessage: withApiPath("/ping"),
    contractList: withApiPath("/contractList"),
    contractCard: (id: string) => withApiPath(`/contractDetails/${id}`),
    submitContract: withApiPath("/submitContract"),
    deleteContract: (id: string) => withApiPath(`/deleteContract/${id}`),
    newContractData: withApiPath("/newContractData")
}