[![Build Status](https://travis-ci.org/matthieudelaro/vie-de-merde-scraper-scala.svg?branch=master)](https://travis-ci.org/matthieudelaro/vie-de-merde-scraper-scala)

# "Vie de Merde" Scraper

This is a Scala/Play project, which parses posts from [Vie de Merde (VDM)](www.viedemerde.fr/news?page=1),
and serves them with a REST API.

Note: There is an API to retrieve VDM posts. It would have been much easier and cleaner to use it.
However, one of the goals of this project was to build a scraper.

## Usage
This section will cover each usage in a dedicated section.
The last subsection covers all usages at once, introducing Docker and Nut.

### Running the scraper
The scraper can be run using the following command line:
`sbt "run-main Scraper"`

Note: It may also be run by querying `/fetch` address.
To enable this extra feature, uncomment the last line of the file `conf/routes`:
```$xslt
# GET        /fetch                      v1.post.PostController.fetch
```

### Serving the API
To start the server, run `sbt run`.
The API is described later in the README.

### Tests
Tests have been implemented in PostControllerSpec. They can be run with:
```$xslt
    SCRALER_SHOULD_MOCK_DB='true' bash -c 'sbt test'
```
This should output such result:
```$xslt
[info] PostControllerSpec:
[info] PostController
[trace] v.p.PostActionBuilder - invokeBlock:
[trace] v.p.PostRepositoryImpl - list:
[info] - should return the list of all posts
[trace] v.p.PostActionBuilder - invokeBlock:
[trace] v.p.PostRepositoryImpl - list:
[info] - should return the list of posts selected by author
[trace] v.p.PostActionBuilder - invokeBlock:
[trace] v.p.PostRepositoryImpl - list:
[info] - should return the list of posts selected by from (date)
[trace] v.p.PostActionBuilder - invokeBlock:
[trace] v.p.PostRepositoryImpl - list:
[info] - should return the list of posts selected by to (date)
[trace] v.p.PostActionBuilder - invokeBlock:
[trace] v.p.PostRepositoryImpl - list:
[info] - should return the list of posts selected by from / to (date)
[trace] v.p.PostActionBuilder - invokeBlock:
[trace] v.p.PostRepositoryImpl - list:
[info] - should return the list of posts selected by author / from / to (date)
[trace] v.p.PostActionBuilder - invokeBlock:
[trace] v.p.PostRepositoryImpl - get: id = id10
[info] - should return the list of the requested index
[info] ScalaTest
[info] Run completed in 13 seconds, 864 milliseconds.
[info] Total number of tests run: 7
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 7, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[info] Passed: Total 7, Failed 0, Errors 0, Passed 7
```

### Docker and Nut
Previous usages can all be run in Docker, thanks to the following commands:
```$xslt
# scrap:
docker run -it --rm --workdir=/root/.ivy2 --volume=/path/to/this/repository:/root/.ivy2 --publish="9000:9000" ysihaoy/scala-play:2.12.3-2.6.2-sbt-0.13.15 sh -c 'sbt "run-main Scraper"'

# run:
docker run -it --rm --workdir=/root/.ivy2 --volume=/path/to/this/repository:/root/.ivy2 --publish="9000:9000" ysihaoy/scala-play:2.12.3-2.6.2-sbt-0.13.15 sh -c "sbt run"

# test:
docker run -it --rm --workdir=/root/.ivy2 --volume=/path/to/this/repository:/root/.ivy2 --env="SCRALER_SHOULD_MOCK_DB=true" ysihaoy/scala-play:2.12.3-2.6.2-sbt-0.13.15 sh -c "sbt test"
```

But wait, there's an easier way than running those long commands. [Nut](https://github.com/matthieudelaro/nut)
has been designed with this use-case in mind:
```$xslt
# 1) Install Nut :
#   Methods: NPM / download binaries / compile from source in a container
#   See https://github.com/matthieudelaro/nut#getting-nut
# 2) Call Nut from the command line in /path/to/this/repository/ :
    nut scrap
    nut run
    nut test
```

You may even run SBT interactive mode in a container with `nut cli`.

## API

### GET /api/posts:
Returns a list of posts. Example:
```Json
{
  "posts": [
    {
      "id": "242095",
      "content": "Dites-moi qui est ce grand tableau noir ? Ringo pourrait presque sortir de sa retraite et faire une nouvelle version de sa chanson, (heureusement) tombée dans les oubliettes du bon goût, pour soutenir ces six professeurs du collège Albert Camus de Gaillac, ainsi que les parents d'élèves qui se sont joints à eux pour sauver des tableaux noirs condamnés à la déchetterie. Pourquoi la police s'est elle intéressée à ce cas, et pourquoi une telle VDM ? Apprenant que les tableaux de leur collège allaient être remplacés par des tableaux blancs interactifs, ils ont voulu les protéger, à l'instar de certains archéologues tels qu'Indiana Jones, en les déplaçant dans un garage proche de l'école… mais sans l'accord de l'adminstration du collège. Erreur de débutant. Quand ils sont revenus chercher leur butin scolaire, ils ont été accueillis par une douzaine de policiers. C'est un peu excessif, non ? Un peu, sauf qu'il s'avère qu'il ne s'agissait pas que de tableaux, mais de mobilier en général, et le rectorat n'entend pas tout ça de la même façon. La caution nostalgie des tableaux leur paraissant suspecte, le rectorat invoque également la prise d'autres éléments comme des \"écrans, des chaises, des tables, des étagères…\" Il est question donc d'une intrusion, car le bâtiment était fermé à clé, chose qui a été formellement démentie par les professeurs. Solidarité Une pétition de soutien envers les enseignants a recueilli déjà plus de 8000 signatures. De notre côté, on espère qu'ils recevront au moins 4 heures de colle et un devoir à la maison.",
      "date": "2017-09-13T09:30:00Z",
      "author": "Professeur Tournesol"
    }
  ],
  "count": 1
}
```


Optional parameters:
- from
- to
- author

Examples:
- /api/posts
- /api/posts/api/posts?from=2017-01-01T00:00:00Z&to=2017-12-31T00:00:00Z
- /api/posts?author=Genius

#### GET /api/posts/<ID>
Returns the desired post. For example:
```Json
{
  "post": {
    "id": "242095",
    "content": "Dites-moi qui est ce grand tableau noir ? Ringo pourrait presque sortir de sa retraite et faire une nouvelle version de sa chanson, (heureusement) tombée dans les oubliettes du bon goût, pour soutenir ces six professeurs du collège Albert Camus de Gaillac, ainsi que les parents d'élèves qui se sont joints à eux pour sauver des tableaux noirs condamnés à la déchetterie. Pourquoi la police s'est elle intéressée à ce cas, et pourquoi une telle VDM ? Apprenant que les tableaux de leur collège allaient être remplacés par des tableaux blancs interactifs, ils ont voulu les protéger, à l'instar de certains archéologues tels qu'Indiana Jones, en les déplaçant dans un garage proche de l'école… mais sans l'accord de l'adminstration du collège. Erreur de débutant. Quand ils sont revenus chercher leur butin scolaire, ils ont été accueillis par une douzaine de policiers. C'est un peu excessif, non ? Un peu, sauf qu'il s'avère qu'il ne s'agissait pas que de tableaux, mais de mobilier en général, et le rectorat n'entend pas tout ça de la même façon. La caution nostalgie des tableaux leur paraissant suspecte, le rectorat invoque également la prise d'autres éléments comme des \"écrans, des chaises, des tables, des étagères…\" Il est question donc d'une intrusion, car le bâtiment était fermé à clé, chose qui a été formellement démentie par les professeurs. Solidarité Une pétition de soutien envers les enseignants a recueilli déjà plus de 8000 signatures. De notre côté, on espère qu'ils recevront au moins 4 heures de colle et un devoir à la maison.",
    "date": "2017-09-13T09:30:00Z",
    "author": "Professeur Tournesol"
  }
}
```
