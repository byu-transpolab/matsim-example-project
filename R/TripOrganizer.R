
library(rmarkdown)
library(tidyverse)

trips <- read_csv("scenarios/provo_orem/trips100.csv")
#trips <- trips %>%
# mutate(depart = ifelse(depart == 9, 0, ifelse(depart == 11, 1, ifelse(depart == 5, 2, depart))))


#make the start of the chronological order be at 3 am. 
#midnight is assumed to be the value of 0, but it could be 24 already
trips <- trips %>%
  mutate(chrono = ifelse(depart != 1 & depart != 2 & depart != 0, depart, ifelse(depart==0, 24, ifelse(depart==1, 25, 26))))


#put in chronological order
nd <- order(trips$chrono, decreasing=F)
trips <- trips[nd,]
trips <- trips[,-14]


write_csv(trips,"scenarios/provo_orem/trips100c.csv")


# We found that times are out of order.
# rearrange the time so that activities are happening in 
# chronological order.

final_trips <- read_csv("scenarios/activitysim_output/final_trips.csv")

# sort by person_id and depart
# look at person 1774266
final_trips %>%
  group_by(person_id) %>%
  arrange(person_id, depart) %>% write_csv("scenarios/activitysim_output/final_trips_chron.csv")
