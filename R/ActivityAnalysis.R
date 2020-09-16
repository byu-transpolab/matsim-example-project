# one afternoon to analyze typical activity characteristics
# from trip data such as start time and duration of activities

library(tidyverse)
data_trips <- read_csv("data/final_trips.csv")

clean_trips <- data_trips %>%
  transmute(
    id = paste(as.character(person_id), "-", as.character(household_id)),
    purpose, depart
  )

activities <- clean_trips %>% 
  arrange(id, depart) %>%
  group_by(id) %>%
  mutate(start = depart,
         end = lead(depart),
         duration = end - start) 

duration_stats <- activities %>%
  group_by(purpose) %>%
  summarise(
    mean = mean(duration, na.rm = T), # average time trips starts to that purpose
    stdev = sd(duration, na.rm = T),
    median = median(duration, na.rm = T),
    min = min(duration, na.rm = T),
    max = max(duration, na.rm = T)
  ) %>%
  mutate(type = "duration")

start_stats <- activities %>%
  group_by(purpose) %>%
  summarise(
    mean = mean(start, na.rm = T), # average time trips starts to that purpose
    stdev = sd(start, na.rm = T),
    median = median(start, na.rm = T),
    min = min(start, na.rm = T),
    max = max(start, na.rm = T)
  ) %>%
  mutate(type = "start")

end_stats <- activities %>%
  group_by(purpose) %>%
  summarise(
    mean = mean(end, na.rm = T), # average time trips starts to that purpose
    stdev = sd(end, na.rm = T),
    median = median(end, na.rm = T),
    min = min(end, na.rm = T),
    max = max(end, na.rm = T)
  ) %>%
  mutate(type = "end")

total <- bind_rows(duration_stats, start_stats,end_stats)

  

  
  
# I want to plot the histograms of start times (eat out has different peak start times for example)
