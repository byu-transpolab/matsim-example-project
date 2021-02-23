library(tidyverse)
library(tidytransit)

gtfs <- read_gtfs("data/gtfs.zip")


uvx_routeid <- "80588"


gtfs$trips  %>%
  filter(route_id == uvx_routeid) %>%
  group_by(service_id) %>%
  tally()

View(gtfs$calendar_dates)
