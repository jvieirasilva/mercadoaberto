#!/bin/bash
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

CLUSTER_NAME="order-app-cluster"
RDS_IDENTIFIER="order-app-db"
ECS_CLUSTER="spring-api-cluster"
ECS_SERVICE="spring-api-service"
REGION="us-east-1"

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}  STOP - RDS + ECS + EKS + ALB + EIP    ${NC}"
echo -e "${BLUE}=========================================${NC}"

# ---- 1. Parar ECS ----
echo -e "\n${BLUE}[1/6] A parar ECS...${NC}"
ECS_EXISTS=$(aws ecs describe-services \
  --cluster $ECS_CLUSTER --services $ECS_SERVICE \
  --region $REGION --query 'services[0].status' --output text 2>/dev/null)

if [ "$ECS_EXISTS" == "ACTIVE" ]; then
  aws ecs update-service --cluster $ECS_CLUSTER --service $ECS_SERVICE \
    --desired-count 0 --region $REGION > /dev/null
  echo -e "${GREEN}✓ ECS service escalado para 0 tasks${NC}"
else
  echo -e "${GREEN}✓ ECS service não encontrado ou já parado${NC}"
fi

# ---- 2. Deletar Load Balancers (ALB) ----
echo -e "\n${BLUE}[2/6] A verificar Load Balancers...${NC}"
ALB_ARNS=$(aws elbv2 describe-load-balancers --region $REGION \
  --query 'LoadBalancers[*].LoadBalancerArn' --output text 2>/dev/null)

if [ -z "$ALB_ARNS" ]; then
  echo -e "${GREEN}✓ Nenhum Load Balancer encontrado${NC}"
else
  for ARN in $ALB_ARNS; do
    NAME=$(aws elbv2 describe-load-balancers --load-balancer-arns $ARN \
      --region $REGION --query 'LoadBalancers[0].LoadBalancerName' --output text)
    echo -e "${YELLOW}  A deletar ALB: $NAME${NC}"
    aws elbv2 delete-load-balancer --load-balancer-arn $ARN --region $REGION
    echo -e "${GREEN}  ✓ ALB $NAME deletado (~\$16-18/mês poupado)${NC}"
  done
fi

# ---- 3. Deletar Cluster EKS ----
echo -e "\n${BLUE}[3/6] A deletar cluster EKS...${NC}"
CLUSTER_EXISTS=$(aws eks describe-cluster --name $CLUSTER_NAME --region $REGION \
  --query 'cluster.status' --output text 2>/dev/null)

if [ -z "$CLUSTER_EXISTS" ]; then
  echo -e "${GREEN}✓ Cluster EKS já não existe${NC}"
else
  echo "  A deletar cluster '$CLUSTER_NAME' (pode demorar 10-15 min)..."
  eksctl delete cluster --name $CLUSTER_NAME --region $REGION
  echo -e "${GREEN}✓ Cluster EKS deletado${NC}"
fi

# ---- 4. Parar RDS ----
echo -e "\n${BLUE}[4/6] A parar RDS...${NC}"
RDS_STATUS=$(aws rds describe-db-instances \
  --db-instance-identifier $RDS_IDENTIFIER \
  --query 'DBInstances[0].DBInstanceStatus' --output text 2>/dev/null)

if [ "$RDS_STATUS" == "stopped" ]; then
  echo -e "${GREEN}✓ RDS já está parado${NC}"
elif [ "$RDS_STATUS" == "available" ]; then
  aws rds stop-db-instance --db-instance-identifier $RDS_IDENTIFIER --region $REGION > /dev/null
  echo -e "${GREEN}✓ RDS a parar...${NC}"
else
  echo "  RDS status: $RDS_STATUS"
fi

# ---- 5. Libertar Elastic IPs soltos ----
echo -e "\n${BLUE}[5/6] A verificar Elastic IPs soltos...${NC}"
EIP_ALLOCS=$(aws ec2 describe-addresses --region $REGION \
  --query 'Addresses[?AssociationId==null].AllocationId' --output text 2>/dev/null)

if [ -z "$EIP_ALLOCS" ]; then
  echo -e "${GREEN}✓ Nenhum EIP solto encontrado${NC}"
else
  for ALLOC_ID in $EIP_ALLOCS; do
    aws ec2 release-address --allocation-id $ALLOC_ID --region $REGION
    echo -e "${GREEN}  ✓ EIP $ALLOC_ID libertado${NC}"
  done
fi

# ---- 6. Deletar NAT Gateways ----
echo -e "\n${BLUE}[6/6] A verificar NAT Gateways...${NC}"
NAT_IDS=$(aws ec2 describe-nat-gateways --region $REGION \
  --query 'NatGateways[?State==`available`].NatGatewayId' --output text 2>/dev/null)

if [ -z "$NAT_IDS" ]; then
  echo -e "${GREEN}✓ Nenhum NAT Gateway encontrado${NC}"
else
  for NAT_ID in $NAT_IDS; do
    echo -e "${YELLOW}  A deletar NAT Gateway: $NAT_ID${NC}"
    aws ec2 delete-nat-gateway --nat-gateway-id $NAT_ID --region $REGION > /dev/null
    echo -e "${GREEN}  ✓ NAT Gateway $NAT_ID deletado (~\$32/mês poupado)${NC}"
  done
fi

# ---- Resultado ----
echo -e "\n${BLUE}=========================================${NC}"
echo -e "${GREEN}  INFRA PARADA!                         ${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "  ✅ ECS parado (0 tasks)"
echo -e "  ✅ ALB(s) verificados/deletados"
echo -e "  ✅ Cluster EKS deletado"
echo -e "  ✅ RDS parado (~\$0.10/dia storage)"
echo -e "  ✅ Elastic IPs libertados"
echo -e "  ✅ NAT Gateways verificados/deletados"
echo -e "\n  ⚠️  RDS reinicia automaticamente após 7 dias!"
