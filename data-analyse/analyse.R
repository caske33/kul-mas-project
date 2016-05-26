library(dplyr)
library(ggplot2)
library(car)
library(userfriendlyscience)

alfa = 0.05

results = read.csv("masresults.csv", sep = ";", header = TRUE)

MASplot = function(xAxis, yAxis, filterQuery = "", title = "", xlabel = xAxis, ylabel = yAxis) {
  if(filterQuery == ""){
    if(title == ""){
      title = "Using all results"
    }
    data = results
  } else {
    if(title == ""){
      title = filterQuery
    }else{
      title = paste(title, "(", filterQuery,")")
    }
    data = results %>% filter_(filterQuery)
  }

  data = data %>%
    group_by_("protocolType", xAxis) %>%
    summarise_(aggregatedY = paste("mean(",yAxis,")"), x=paste("first(",xAxis,")"), se = paste("sd(",yAxis,")/sqrt(n())"), n = "n()")

  ggplot(data, aes(x=x, y=aggregatedY, col = protocolType)) +
    geom_line() +
    geom_point() +
    geom_errorbar(aes(ymin = aggregatedY - se*qt(1-alfa/2, df=n), ymax = aggregatedY + se*qt(1-alfa/2, df=n)), width = .3) +
    xlab(xlabel) +
    ylab(ylabel) +
    ggtitle(title)
}
MASplot("nbDrones", "totalProfit")
MASplot("nbWarehouses", "totalProfit")
MASplot("nbInitialClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")

MASplot("nbWarehouses", "totalProfit", "nbDrones >= 7")
MASplot("nbInitialClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")

MASplot("nbDrones", "averageDeliveryTime")
MASplot("nbWarehouses", "averageDeliveryTime")
MASplot("nbInitialClients", "averageDeliveryTime")
MASplot("nbDynamicClients", "averageDeliveryTime")

MASplot("nbDrones", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbWarehouses", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbInitialClients", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbDynamicClients", "averageDeliveryTime", "nbDrones >= 7")

MASplot("nbDrones", "nbClientsNotDelivered")
MASplot("nbWarehouses", "nbClientsNotDelivered")
MASplot("nbInitialClients", "nbClientsNotDelivered")
MASplot("nbDynamicClients", "nbClientsNotDelivered")
MASplot("nbDrones", "nbClientsNotDelivered", "nbInitialClients < 20 & nbDynamicClients < 50")
MASplot("nbInitialClients", "nbClientsNotDelivered", "nbDrones >= 7")
MASplot("nbInitialClients", "nbClientsNotDelivered", "nbDrones < 7")
MASplot("nbDynamicClients", "nbClientsNotDelivered", "nbDrones < 7")
MASplot("nbDynamicClients", "nbClientsNotDelivered", "nbDrones >= 7")


#############################################################
# Question 1: difference in profit?
MASplot("nbDrones", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 7")
# <4, >7 en ertussen

# >= 7
subset = results %>% filter(nbDrones >= 7)
leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# verschil tussen alle 3

# ertussen
subset = results %>% filter(nbDrones < 7 & nbDrones >= 4)
leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# verschil tussen alle 3

# < 4
subset = results %>% filter(nbDrones < 4)
leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# geen verschil tussen CNET en CNCP

# Hyptothese:
# < 4: fout: DynCNET minder profit, en CNCP=CNET
# ertussen: fout DynCNET maakt minder profit
# >= 7: correct
#############################################################

#############################################################
# Question 2: difference in averageDeliveryTime?
MASplot("nbDrones", "averageDeliveryTime")
#=> Consider 1, 2 en 3 drones

# 1 Drone
subset = results %>% filter(nbDrones == 1)
leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 1 Drone: geen verschil tussen CNET en CNCP, wel tussen DynCNET en andere 2

# 2 Drone
MASplot("nbDrones", "averageDeliveryTime", "nbDrones == 2")
subset = results %>% filter(nbDrones == 2)
leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 2 Drones: verschil tussen alle 3!

# 3 Drone
subset = results %>% filter(nbDrones == 3)
leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 3 Drones: verschil tussen alle 3!

# Hypothese:
# - fout voor 1 Drone, daar delivert DynCNET sneller dan andere 2, andere 2 wel gelijk
# - fout voor >1 Drone: daar delivert CNCP sneller dan CNET, niet gelijk
#############################################################



#############################################################
# Question 3: difference in nbClientsNotDelivered?
MASplot("nbDrones", "nbClientsNotDelivered")
MASplot("nbDrones", "nbClientsNotDelivered", "nbDrones > 5")
MASplot("nbDrones", "nbClientsNotDelivered", "nbInitialClients < 20 & nbDynamicClients < 50")
# => Complex situation (interaction between Drones and clients)

# Not too many clients
subset = results %>% filter(nbInitialClients < 20 & nbDynamicClients < 50)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# => Difference between all 3

# 10 Drones
subset = results %>% filter(nbDrones == 10)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNET onder DynCNET

# 9 Drones
subset = results %>% filter(nbDrones == 9)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# geen verschil tussen CNET en DynCNET

# 4 Drones
subset = results %>% filter(nbDrones == 4)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# wel verschil tussen CNET en DynCNET

# 8 Drones
subset = results %>% filter(nbDrones == 8)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# DynCNET < CNET

# <=2 Drones
subset = results %>% filter(nbDrones <= 2)
leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNET = CNCP

# Hypothese:
# - Only when nbDrones is low, will CNET and CNCP have the same number of clients not delivered, otherwise, CNCP will outperform CNCP
# - When number of clients and drones increase, CNET and DynCNET can switch in number of clients delivered, in some situations they equal
#############################################################







# Old stuff
s = results %>% filter(nbDrones == 8 & nbWarehouses == 10)
# Test for homoskedasticiteit: mag niet significant zijn, Pr(>F) moet >0.05 zijn, of er mogen dus geen sterretje(s) naast resultaat staan
# Anders werkt TukeyHSD NIET!
leveneTest(totalProfit ~ protocolType, s)
boxplot(totalProfit ~ protocolType, s)
s.aov = aov(totalProfit ~ protocolType, s)
summary(s.aov)
TukeyHSD(s.aov)
posthocTGH(s$totalProfit, s$protocolType, method="tukey")
posthocTGH(s$totalProfit, s$protocolType, method="games-howell")

s = results %>% filter(nbDrones == 3 & nbDynamicClients == 40 & nbInitialClients == 10 & nbWarehouses == 8)
boxplot(totalProfit ~ protocolType, s)
leveneTest(totalProfit ~ protocolType, s)
s.aov = aov(totalProfit ~ protocolType, s)
summary(s.aov)
TukeyHSD(s.aov)
posthocTGH(s$totalProfit, s$protocolType, method="tukey")
posthocTGH(s$totalProfit, s$protocolType, method="games-howell")


