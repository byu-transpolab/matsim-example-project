
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
