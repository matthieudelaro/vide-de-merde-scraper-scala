# "Vie de Merde" Scraper

This is a Scala/Play project, which parses posts from [Vide de Merde (VDM)](www.viedemerde.fr/news?page=1),
and serves them with a REST API.

Note: There is an API to retrieve VDM posts, which would have been much cleaner to use.
However, one of the goals of this project was to build a scraper.

### Running

#### The scraper
The scraper can be run using the following command line: 
`sbt "run-main Scraper"`

Note: It may also be run by querying `/fetch` address.
To enable this extra feature, uncomment the last line of the file `conf/routes`:
```$xslt
# GET        /fetch                      v1.post.PostController.fetch
```  

#### The server 

### Usage

### API

#### GET /api/posts:
Optional parameters:
- from  
- to
- author

Example: 
- /api/posts
- /api/posts/api/posts?from=2017-01-01T00:00:00Z&to=2017-12-31T00:00:00Z
- /api/posts?author=Genius

```
and get json TODO
```

#### GET /api/posts/<ID>

```
and get json TODO
```

## Limitations / Features
Currently, some posts such as the following [Best of The Week](www.viedemerde.fr/article/et-la-vdm-qui-vous-a-fait-le-plus-rire-cette-semaine-est_234026.html)
are ignored while scraping the website. Reasons for this choice is that they refer previous posts, 
which implies duplicate information in the database.