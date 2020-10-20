import Config from '../Config'

export default {
    pingMessage: `${Config.appUrl}/ping`,
    contractList: `${Config.appUrl}/contractList`,
    contractCard: (id: string) => `${Config.appUrl}/contractDetails/${id}`,
    submitContract: `${Config.appUrl}/submitContract`
}