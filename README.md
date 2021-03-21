# Trading Journal

My humble pet-project. This is web-service for accounting binary options' contracts.

Key features:
* CRUD for contracts.
* Autofill contract parameters with info from trading broker through API (only binary.com now).
* Basic statistic on daily, weekly and monthly basis.

Technologies:
* Backend: Scala, Play Framework. Database: PostgreSQL.
* Frontend: TypeScript, Vue.js. UI: Element.

Future plans:
* Rewrite auth system, current one is terrible.
* Rewrite backend completely with FP-stack (cats, cats-effect, zio, quill + doobie, tapir, etc).
* Remove myriads of TODOs.
* Improve frontend logic.
* Improve project in general.
