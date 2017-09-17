# "Vie de Merde" Scraper

This is a Scala/Play project, which parses posts from [Vide de Merde (VDM)](www.viedemerde.fr/news?page=1),
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
    # TODO: mocking the PostRepository to load directly from database_posts_mock.json would be much better
    mv database_posts.json database_posts.save.json
    cp test/database_posts_mock.json database_posts.json
    sbt test
    rm database_posts.json
    mv database_posts.save.json database_posts.json
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
docker run -it --rm --workdir=/root/.ivy2 --volume=/path/to/this/repository:/root/.ivy2 --env="TZ=UTC-02:00" --publish="9000:9000" ysihaoy/scala-play:2.12.3-2.6.2-sbt-0.13.15 sh -c 'sbt "run-main Scraper"'

# run:
docker run -it --rm --workdir=/root/.ivy2 --volume=/path/to/this/repository:/root/.ivy2 --env="TZ=UTC-02:00" --publish="9000:9000" ysihaoy/scala-play:2.12.3-2.6.2-sbt-0.13.15 sh -c "sbt run"

# test:
docker run -it --rm --workdir=/root/.ivy2 --volume=/path/to/this/repository:/root/.ivy2 --env="TZ=UTC-02:00" --publish="9000:9000" ysihaoy/scala-play:2.12.3-2.6.2-sbt-0.13.15 sh -c 'mv database_posts.json database_posts.save.json; cp test/database_posts_mock.json database_posts.json; sbt test; rm database_posts.json; mv database_posts.save.json database_posts.json'
```

But wait, there's an easier way than running those long commands. [Nut](https://github.com/matthieudelaro/nut)
has been designed with this use-case in mind:
```$xslt
# 1) Install Nut : 
#   Methods: NPM / download binaries / compile from source in a container
#   See https://github.com/matthieudelaro/nut#getting-nut 
# 2) Call Nut:
    nut scrap
    nut run
    nut test
```

You may even run SBT interactive mode in a container with `nut cli`.

## API

### GET /api/posts:
Optional parameters:
- from  
- to
- author

Example: 
- /api/posts
- /api/posts/api/posts?from=2017-01-01T00:00:00Z&to=2017-12-31T00:00:00Z
- /api/posts?author=Genius

Returns a list of posts.

#### GET /api/posts/<ID>
Returns the desired post.

## Limitations / Features
Currently, some posts such as the following [Best of The Week](www.viedemerde.fr/article/et-la-vdm-qui-vous-a-fait-le-plus-rire-cette-semaine-est_234026.html)
are ignored while scraping the website. Reasons for this choice is that they refer previous posts, 
which implies duplicate information in the database.