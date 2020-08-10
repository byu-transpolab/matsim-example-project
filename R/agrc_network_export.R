library(sf)
library(tidyverse)
library(tigris)

# This file contains code to download and extract street network data from AGRC,
# and transit data for UTA.

# Set up boundaries ========
scope <- pumas("UT", class = "sf") %>%
  filter(PUMACE10 %in% c("49002", "49003")) %>%
  st_transform(4326)
# scope <- tracts("UT", "Utah", class = "sf") %>%
#   filter(NAME %in% c("24", "19", "18.01", "18.02", "18.03")) %>%
#   st_transform(4326)

scenario_name <- "provo_orem"
if(!dir.exists(file.path("scenarios", scenario_name))){
  dir.create(file.path("scenarios", scenario_name))
}

# Get AGRC Network data ==========
# The Utah AGRC multimodal network database is available at 
#   https://gis.utah.gov/data/transportation/street-network-analysis/#MultimodalNetwork
#   
# For this research we downloaded the information from that file in August 2020.
# The file we downloaded is available on Box, but is not committed to the 
# repository for space reasons. This file contains code to download the archived 
# AGRC network, extract it. 
filegdb <- "data/MM_NetworkDataset_07272020.gdb"
if(!file.exists(filegdb)) {
  zippedgdb <- "data/agrc_network.zip"
  if(!file.exists(zippedgdb)) {
    download.file("https://byu.box.com/shared/static/qyjf1of9dau7tyc3rptxc9w4fi08x3d6.zip",
      zippedgdb)
  }
  system2("7z", c("e", zippedgdb, str_c("-o", filegdb)) )
} 
  


# Pull network data from GDB ===============
agrc_layers <- st_layers(filegdb)

# get nodes
nodes <- st_read(filegdb, layer = "NetworkDataset_ND_Junctions") %>%
  st_transform(4326) %>%
  mutate(id = row_number())

# get auto_links
links <- st_read(filegdb, layer = "AutoNetwork") %>%
  st_transform(4326)



  

# Set up geographic scope ========
scope_nodes <- nodes %>% 
  st_filter(scope) 

scope_links <- links %>%
  mutate(link_id = row_number(), AADT = ifelse(AADT == 0, NA, AADT)) %>%
  select(link_id, Oneway, Speed, DriveTime, Length_Miles, RoadClass, AADT) %>%
  st_filter(scope)

# plot to visualize links
# ggplot(scope_links, aes(color = RoadClass)) + 
#   geom_sf()

# Node identification =======
# The links don't have any node information on them. So let's extract the
# first and last points from each polyline. This actually extracts all of them
link_points <- scope_links %>%
  select(link_id) %>%
  st_cast("POINT") # WARNING: will generate n points for each point.
  
# now we get the first point of each feature and find the nearest node
start_nodes <- link_points %>% group_by(link_id) %>% slice(1) %>%
  st_join(scope_nodes, join = st_nearest_feature) %>%
  rename(start_node = id)
# and do the same for the last n() point of each feature
end_nodes <- link_points %>% group_by(link_id) %>% slice(n()) %>%
  st_join(scope_nodes, join = st_nearest_feature) %>%
  rename(end_node = id)

# finally, we put this back on the data
mylinks <- scope_links %>%
  left_join(start_nodes %>% st_set_geometry(NULL), by = "link_id") %>%
  left_join(end_nodes   %>% st_set_geometry(NULL), by = "link_id")


# Capacity ===================
# The data have no lane information, which is pretty criminal. But we'll make
# due with some defaults
hcmr_lookup <- read_csv("data/hcmr_lookup.csv.zip")

# well, we end up needing to use lots of defaults. 
link_attributes <- tibble(
  RoadClass = c(
    "1 Interstates", 
    "2 US Highways, Separated",  "3 US Highways, Unseparated", 
    "4 Major State Highways, Separated",  "5 Major State Highways, Unseparated", 
    "6 Other State Highways (Institutional)",  "7 Ramps, Collectors", 
    "8 Major Local Roads, Paved",  "9 Major Local Roads, Not Paved",
    "10 Other Federal Aid Eligible Local Roads", 
    "11 Other Local, Neighborhood, Rural Roads",  "12 Other"
  ),
  ft = c(
    "Freeway",  "MLHighway", "PrArterial", 
    "MLHighway", "PrArterial", "MinArterial", "MinArterial",
    "MinArterial", "MinArterial", "Local", "Local", "Local"
  ),
  lanes = c(
    4,  3, 2, 3, 2,  2, 2,  2, 2,  1, 1, 1
  ),
  sl = c(
    65, 55, 50, 50, 45, 40, 35, 30, 20, 25, 25, 25
  ),
  med = c(
    "Restrictive", 
    "Restrictive", "NonRestrictive",
    "Restrictive", "NonRestrictive",
    "NonRestrictive", "NonRestrictive",
    "NonRestrictive", "NonRestrictive",
    "None", "None", "None"
  ),
  at = "Suburban", terrain = "level"
)

mylinks <- mylinks %>%
  left_join(link_attributes) %>%
  left_join(hcmr_lookup) %>%
  mutate(
    Oneway = case_when(
      Oneway == "B" ~ 0,
      TRUE ~ 1
    )
  )


# plot to visualize capacity
# ggplot(mylinks_capacity, aes(x = capacity * 10, y = AADT)) + geom_point() +
#   geom_abline(slope = 1, intercept = 0, lty = "dotted") + 
#   scale_x_log10() + scale_y_log10()
# 
# ggplot(mylinks_capacity, aes(color = capacity)) + geom_sf()


# Write out ===================
st_write(mylinks, file.path("scenarios", scenario_name, "network.geojson"), delete_dsn = TRUE)
write_csv(mylinks %>% st_set_geometry(NULL), file.path("scenarios", scenario_name, "links.csv"))

scope_nodes %>%
  mutate(
    x = st_coordinates(.)[, 1],
    y = st_coordinates(.)[, 2]
  ) %>%
  st_set_geometry(NULL) %>%
  write_csv(file.path("scenarios", scenario_name, "nodes.csv"))
  
