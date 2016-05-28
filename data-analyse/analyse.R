library(plyr)
library(dplyr)
library(ggplot2)
library(car)
library(userfriendlyscience)

label.CNET = "CNET"
label.CNCP = "CNCP"
label.DynCNET = "DynCNET"

results = read.csv("masresults.csv", sep = ";", header = TRUE)
results$protocolType = revalue(results$protocolType, c("DYNAMIC_CONTRACT_NET"=label.DynCNET, "CONTRACT_NET"=label.CNET, "CONTRACT_NET_CONFIRMATION"=label.CNCP))
grid2 = read.csv("grid2.csv", sep = ";", header = TRUE)
grid2$protocolType = revalue(grid2$protocolType, c("DYNAMIC_CONTRACT_NET"=label.DynCNET, "CONTRACT_NET"=label.CNET, "CONTRACT_NET_CONFIRMATION"=label.CNCP))

MASplot = function(xAxis, yAxis, filterQuery = "", title = "", xlabel = xAxis, ylabel = yAxis, use.grid2 = FALSE, filename = "") {
  alfa = 0.05

  if(use.grid2){
    data = grid2
  } else {
    data = results
  }

  if(filterQuery != ""){
    if(title == ""){
      title = filterQuery
    }
    data = data %>% filter_(filterQuery)
  }

  #if(use.grid2){
  #  title = paste(title, "over grid2")
  #}

  data = data %>%
    group_by_("protocolType", xAxis) %>%
    summarise_(aggregatedY = paste("mean(",yAxis,")"), x=paste("first(",xAxis,")"), se = paste("sd(",yAxis,")/sqrt(n())"), n = "n()")

  plot = ggplot(data, aes(x=x, y=aggregatedY, col = protocolType)) +
    geom_line() +
    geom_point() +
    geom_errorbar(aes(ymin = aggregatedY - se*qt(1-alfa/2, df=n), ymax = aggregatedY + se*qt(1-alfa/2, df=n)), width = .3) +
    xlab(xlabel) +
    ylab(ylabel) +
    ggtitle(title)

  if(filename != ""){
    ggsave(file = paste("../verslag/images/",filename,".pdf", sep = ""), plot = plot)
  }

  plot
}
#MASqqplot = function(subset, field) {
#  subsetCNET = unlist((subset %>% filter(protocolType == label.CNET))[field])
#  subsetCNCP = unlist((subset %>% filter(protocolType == label.CNCP))[field])
#  subsetDyn = unlist((subset %>% filter(protocolType == label.DynCNET))[field])
#
#  qqnorm(subsetCNET, main = "CNET")
#  qqline(subsetCNET)
#  qqnorm(subsetCNCP, main = "CNCP")
#  qqline(subsetCNCP)
#  qqnorm(subsetDyn, main = "DynCNET")
#  qqline(subsetDyn)
#}
#MASqqplot(subset, "totalProfit")

MASplot("nbDrones", "totalProfit")
MASplot("nbWarehouses", "totalProfit")
MASplot("nbInitialClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")
MASplot("nbWarehouses", "totalProfit", "nbDrones >= 7")
MASplot("nbInitialClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")
#############################################################
# Question 1: difference in profit?
MASplot("nbDrones", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 7")
# <4, >7 en ertussen

# >= 7
subset = results %>% filter(nbDrones >= 7)
#leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# verschil tussen alle 3

# ertussen
subset = results %>% filter(nbDrones < 7 & nbDrones >= 4)
#leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# verschil tussen alle 3

# < 4
subset = results %>% filter(nbDrones < 4)
#leveneTest(totalProfit ~ protocolType, subset)
posthocTGH(subset$totalProfit, subset$protocolType, method="games-howell");
# geen verschil tussen CNET en CNCP

# Hyptothese:
# < 4: fout: DynCNET minder profit, en CNCP=CNET
# ertussen: fout DynCNET maakt minder profit
# >= 7: correct
#############################################################


MASplot("nbDrones", "averageDeliveryTime")
MASplot("nbWarehouses", "averageDeliveryTime")
MASplot("nbInitialClients", "averageDeliveryTime")
MASplot("nbDynamicClients", "averageDeliveryTime")
MASplot("nbDrones", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbWarehouses", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbInitialClients", "averageDeliveryTime", "nbDrones >= 7")
MASplot("nbDynamicClients", "averageDeliveryTime", "nbDrones >= 7")
#############################################################
# Question 2: difference in averageDeliveryTime?
MASplot("nbDrones", "averageDeliveryTime")
#=> Consider 1, 2 en 3 drones

# 1 Drone
subset = results %>% filter(nbDrones == 1)
#leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 1 Drone: geen verschil tussen CNET en CNCP, wel tussen DynCNET en andere 2

# 2 Drone
MASplot("nbDrones", "averageDeliveryTime", "nbDrones == 2")
subset = results %>% filter(nbDrones == 2)
#leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 2 Drones: verschil tussen alle 3!

# 3 Drone
subset = results %>% filter(nbDrones == 3)
#leveneTest(averageDeliveryTime ~ protocolType, subset)
posthocTGH(subset$averageDeliveryTime, subset$protocolType, method="games-howell");
# voor 3 Drones: verschil tussen alle 3!

# Hypothese:
# - fout voor 1 Drone, daar delivert DynCNET sneller dan andere 2, andere 2 wel gelijk
# - fout voor >1 Drone: daar delivert CNCP sneller dan CNET, niet gelijk
#############################################################


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
# Question 3: difference in nbClientsNotDelivered?
MASplot("nbDrones", "nbClientsNotDelivered")
MASplot("nbDrones", "nbClientsNotDelivered", "nbDrones > 5")
MASplot("nbDrones", "nbClientsNotDelivered", "nbInitialClients < 20 & nbDynamicClients < 50")
MASplot("nbDynamicClients", "nbClientsNotDelivered", "nbInitialClients == 25", use.grid2 = T)
# => Complex situation (interaction between Drones and clients)

# Not too many clients
subset = results %>% filter(nbInitialClients < 20 & nbDynamicClients < 50)
#leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNCP < CNET < DynCNET

# 10 Drones
subset = results %>% filter(nbDrones == 10)
#leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNCP < CNET < DynCNET

# 9 Drones
subset = results %>% filter(nbDrones == 9)
#leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNCP <  CNET = DynCNET

# 8 Drones
subset = results %>% filter(nbDrones == 8)
#leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNCP < DynCNET < CNET

# 4 Drones
subset = results %>% filter(nbDrones == 4)
#leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNCP < CNET < DynCNET

# <=2 Drones
subset = results %>% filter(nbDrones <= 2)
#leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# CNET = CNCP < DynCNET

# Large number of clients
subset = grid2 %>% filter(nbDynamicClients > 100)
#leveneTest(nbClientsNotDelivered ~ protocolType, subset)
posthocTGH(subset$nbClientsNotDelivered, subset$protocolType, method="games-howell");
# => Difference between all 3, DynCNET < CNCP < CNET

# Hypothese:
# - Only when nbDrones is low, will CNET and CNCP have the same number of clients not delivered, otherwise, CNCP will outperform CNCP
# - When number of clients and drones increase, CNET and DynCNET can switch in number of clients delivered, in some situations they equal
#############################################################


MASplot("nbDrones", "nbMessages")
MASplot("nbWarehouses", "nbMessages")
MASplot("nbInitialClients", "nbMessages")
MASplot("nbDynamicClients", "nbMessages")
MASplot("nbDrones", "nbMessages", "protocolType %in% c('CNET', 'CNCP')")
MASplot("nbDynamicClients", "nbMessages", "protocolType %in% c('CNET', 'CNCP')")
MASplot("nbInitialClients", "nbMessages", "protocolType %in% c('CNET', 'CNCP')")
MASplot("nbWarehouses", "nbMessages", "protocolType %in% c('CNET', 'CNCP')")
#############################################################
# Question 4: difference in nbMessages?
MASplot("nbDynamicClients", "nbMessages")
MASplot("nbInitialClients", "nbMessages")
MASplot("nbDrones", "nbMessages")
MASplot("nbDrones", "nbMessages", "nbDynamicClients == 10 & nbInitialClients == 5")
# as nbClients rises, nbMessages rise exponential, instead of linear (but not research question!)
# as nbDrones rises, nbMessages drops

# little clients
subset = results %>% filter(nbDynamicClients == 10 & nbInitialClients == 5)
#leveneTest(nbMessages ~ protocolType, subset)
posthocTGH(subset$nbMessages, subset$protocolType, method="games-howell");
# no difference between CNET and CNCP

# 1 nbDrones
subset = results %>% filter(nbDrones == 2)
#leveneTest(nbMessages ~ protocolType, subset)
posthocTGH(subset$nbMessages, subset$protocolType, method="games-howell");
# no difference between CNET and CNCP

# no filter
subset = results
#leveneTest(nbMessages ~ protocolType, subset)
posthocTGH(subset$nbMessages, subset$protocolType, method="games-howell");
# difference between CNET and CNCP: CNCP MEER messages nodig

# Hypothese:
#   - fout: CNCP heeft wel meer messages nodig dan CNET
#   - correct: DynCNET heeft inderdaad wel meer messages nodig dan andere 2
#############################################################


MASplot("nbDrones", "totalProfit")
MASplot("nbWarehouses", "totalProfit")
MASplot("nbInitialClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")
MASplot("nbWarehouses", "totalProfit", "nbDrones >= 7")
MASplot("nbInitialClients", "totalProfit", "nbDrones >= 7")
MASplot("nbDynamicClients", "totalProfit", "nbDrones >= 7")
MASplot("nbWarehouses", "totalProfit", "nbDrones < 5")
MASplot("nbInitialClients", "totalProfit", "nbDrones < 5")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 5")
#############################################################
# Question 5: possible to make a profit?
MASplot("nbDrones", "totalProfit")
# => genoeg drones nodig!
MASplot("nbWarehouses", "totalProfit", "nbDrones < 5 & nbDynamicClients < 50")
# => als er niet genoeg drones zijn, moeten er wel genoeg warehouses zijn

MASplot("nbInitialClients", "totalProfit", "nbDrones < 4")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 5")
# als er te veel clients zijn (voor het aantal drones), dan gaat het mis

MASplot("nbInitialClients", "totalProfit", "nbDrones == 5")
MASplot("nbDynamicClients", "totalProfit", "nbDrones == 5")
MASplot("nbDynamicClients", "totalProfit", "nbDrones < 7")
# Dynamic kent wel een breakdown!

# Hypothese:
#   - correct: given the right number of drones, warehouses and clients this is possible
#############################################################


MASplot("nbDrones", "totalProfit")
#############################################################
# Question 6: influence of nbDrones on profit
MASplot("nbDrones", "totalProfit")
MASplot("nbDrones", "totalProfit", "nbDynamicClients == 50 & nbInitialClients == 25")

# Hypothese:
#  - as saturation starts to set in quickly, it's not clear if it's really linear, even with maximum number of clients
#############################################################

#############################################################
# Question 7: influence of nbWarehouses on profit
MASplot("nbWarehouses", "totalProfit")
MASplot("nbWarehouses", "totalProfit", "nbDrones < 3")

subset1 = results %>% filter(nbDrones < 3 & nbWarehouses == 8)
subset2 = results %>% filter(nbDrones < 3 & nbWarehouses == 9)
var.test(subset1$totalProfit, subset2$totalProfit)
t.test(subset1$totalProfit, subset2$totalProfit, var.equal = TRUE)
# no increase from 8 to 9 warehouses for 1 or 2 drones

# Hypothese:
# => in some cases, adding 1 warehouse does not help, but in general it helps
#############################################################

#############################################################
# Question 8: influence of nbClients on profit
MASplot("nbInitialClients", "totalProfit")
MASplot("nbDynamicClients", "totalProfit")
# => correct

MASplot("nbInitialClients", "totalProfit", use.grid2 = TRUE)
MASplot("nbDynamicClients", "totalProfit", use.grid2 = TRUE)
# => correct, DynCNET saturates first and collapses harder

MASplot("nbInitialClients", "averageNbSwitchesPerClient", use.grid2 = TRUE)
MASplot("nbDynamicClients", "averageNbSwitchesPerClient", use.grid2 = TRUE)
# => gemiddeld 70% van clients switcht 1 keer van drone, bij saturatie
MASplot("nbInitialClients", "averageNbSwitchesPerDrone", use.grid2 = TRUE)
MASplot("nbDynamicClients", "averageNbSwitchesPerDrone", use.grid2 = TRUE)
# aantal switches per drone stijgt lineair met aantal clients

# Hypothese:
#  - hypothese was correct, maar DynCNET crasht echt
#############################################################


##########################
##########################
####   -------------   ###
####   PLOTS VERSLAG   ###
####   -------------   ###
##########################
##########################
MASplot("nbDrones", "totalProfit", title = "Influence of drones on profit",xlabel = "Number of drones", ylabel = "Profit [€]", filename = "drones-profit")
MASplot("nbWarehouses", "totalProfit", title = "Influence of warehouses on profit",xlabel = "Number of warehouses", ylabel = "Profit [€]", filename = "warehouses-profit")
MASplot("nbInitialClients", "totalProfit", title = "Influence of initial clients on profit",xlabel = "Number of initial clients", ylabel = "Profit [€]", filename = "initialclients-profit")
MASplot("nbDynamicClients", "totalProfit", title = "Influence of extra clients on profit",xlabel = "Number of extra clients", ylabel = "Profit [€]", filename = "dynamicclients-profit")
MASplot("nbDynamicClients", "totalProfit", filter = "nbDrones >= 7", title = "No breakdown for large number of drones (7 or more)", xlabel = "Number of extra clients", ylabel = "Profit [€]", filename = "dynamicclients-profit-largeNbDrones")

MASplot("nbDrones", "averageDeliveryTime", title = "Influence of number of drones on delivery time", xlabel = "Number of drones", ylabel = "average delivery time [ms]", filename = "drones-deliverytime")

MASplot("nbDynamicClients", "nbClientsNotDelivered", "nbInitialClients == 25 & nbDynamicClients < 100", xlabel = "number of extra clients", ylabel = "number of clients not delivered", title = "Influence of number of extra clients on the number of clients that er delivered", use.grid2 = T, filename = "dynamicclients-delivered-grid2")

MASplot("nbDynamicClients", "totalProfit", title = "Influence of number of extra clients on the profit (experiment2)", xlabel = "number of extra clients", ylabel = "Profit [€]", use.grid2 = TRUE ,filename = "dynamicclients-profit-grid2")
