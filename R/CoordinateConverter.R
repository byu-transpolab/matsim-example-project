
library(rmarkdown)
library(tidyverse)
library(data.table)

# Looks like this code adds the household coordinates from 
# hhcoord found in the population_wfrc repo and ties them 
# to the synthetic popoulation and synthetic households
# I am assuming this happens before we run activitysim?
# that would mean that the columns would need to be added
# in the config folder...

# ActivitySim does not keep the coordinates. So in order for
# the plansMaker to work, the households file needs coordinates
# (an option would be to add the hhcoord file to the plans maker
# and have the join happen inside java... -Nate)

coord <- read_csv("data/hhcoord.csv")

# the goal is to write coordinates onto households
fhouse <- read_csv("data/final_households.csv")
comb2 <- left_join(fhouse,coord, by = "household_id")
colnames(comb2)[2] <- "TAZ"
write_csv(comb2,"scenarios/activitysim_output/final_households_wc.csv")

# see if households got the coordinates
comb2 %>% select(household_id, TAZ, longitude, latitude)

fperson <- read_csv("data/final_persons_wc.csv")
fperson %>% mutate(wc_var = ifelse(is.na(wc_var), F, wc_var)) %>%
  write_csv("scenarios/activitysim_output/final_persons_wc.csv")

# check for outliers
# ---- THERE ARE 20 NA HOUSEHOLDS -----------
# we should probably just erase them.
missing <- comb2 %>% select(household_id, TAZ, longitude, latitude) %>%
  filter(is.na(latitude))

coord %>% filter(household_id %in% missing$household_id)



## ============= Samples and examples ==========================================

# xy plot
ggplot(data = coord %>% sample_n(10000), aes(x = longitude, y=latitude)) + geom_point()

sperson <- read_csv("coordinates/synthetic_persons2.csv")
comb <- left_join(coord,sperson, by = "household_id") 
  comb <- comb[,-23]
  colnames(comb)[2] <- "TAZ"
write_csv(comb,"coordinates/synthetic_persons_xy.csv")

fhouse <- read_csv("scenarios/activitysim_output/sample/sample_households.csv")
comb2 <- left_join(fhouse,coord, by = "household_id")
  #comb2 <- comb2[,-14]
  colnames(comb2)[2] <- "TAZ"
write_csv(comb2,"scenarios/activitysim_output/sample/sample_households.csv")

# see if households got the coordinates
comb2 %>% select(household_id, TAZ, longitude, latitude)

fperson <- read_csv("scenarios/activitysim_output/sample/sample_persons.csv")
fperson %>% mutate(wc_var = ifelse(is.na(wc_var), F, wc_var)) %>%
  write_csv("scenarios/activitysim_output/sample/sample_persons.csv")
