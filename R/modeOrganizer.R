# Mode Organizer
# This helps to visualize and organize the modes from 
# activity sim to match them with the built-in modes
# in MATSim

library(tidyverse)

# read_csv("scenarios/provo_orem/facility_ids.csv")
data_trips <- read_csv("data/final_trips.csv")

data_trips %>% group_by(trip_mode) %>%
  tally() %>%
  add_column(matsim = c("bike", rep("pt", 4), rep("car", 3), "walk", rep("pt", 4)))

