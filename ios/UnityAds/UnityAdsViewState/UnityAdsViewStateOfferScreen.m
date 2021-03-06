//
//  UnityAdsViewStateOfferScreen.m
//  UnityAds
//
//  Created by Pekka Palmu on 4/4/13.
//  Copyright (c) 2013 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewStateOfferScreen.h"

#import "../UnityAdsWebView/UnityAdsWebAppController.h"
#import "../UnityAdsCampaign/UnityAdsCampaignManager.h"
#import "../UnityAdsProperties/UnityAdsConstants.h"
#import "../UnityAdsItem/UnityAdsRewardItem.h"
#import "../UnityAdsView/UnityAdsMainViewController.h"
#import "../UnityAds.h"

#import "../UnityAdsZone/UnityAdsZoneManager.h"
#import "../UnityAdsZone/UnityAdsIncentivizedZone.h"

@implementation UnityAdsViewStateOfferScreen

- (UnityAdsViewStateType)getStateType {
  return kUnityAdsViewStateTypeOfferScreen;
}

- (void)enterState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super enterState:options];
  [self placeToViewHiearchy];
}

- (void)willBeShown {
  [super willBeShown];
  id currentZone = [[UnityAdsZoneManager sharedInstance] getCurrentZone];
  if([currentZone isIncentivized]) {
    id itemManager = [((UnityAdsIncentivizedZone *)currentZone) itemManager];
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIOpen, kUnityAdsWebViewDataParamZoneKey: [currentZone getZoneId], kUnityAdsItemKeyKey:[itemManager getCurrentItem].key}];
  } else {
    [[UnityAdsWebAppController sharedInstance] setWebViewCurrentView:kUnityAdsWebViewViewTypeStart data:@{kUnityAdsWebViewAPIActionKey:kUnityAdsWebViewAPIOpen, kUnityAdsWebViewDataParamZoneKey: [currentZone getZoneId]}];
  }
  
  [self placeToViewHiearchy];
}

- (void)wasShown {
  [super wasShown];
}

- (void)exitState:(NSDictionary *)options {
  UALOG_DEBUG(@"");
  
  [super exitState:options];
}

- (void)placeToViewHiearchy {
  if (![[[[UnityAdsWebAppController sharedInstance] webView] superview] isEqual:[[UnityAdsMainViewController sharedInstance] view]]) {
    [[[UnityAdsMainViewController sharedInstance] view] addSubview:[[UnityAdsWebAppController sharedInstance] webView]];
    [[[UnityAdsWebAppController sharedInstance] webView] setFrame:[[UnityAdsMainViewController sharedInstance] view].bounds];
    
    [[[UnityAdsMainViewController sharedInstance] view] bringSubviewToFront:[[UnityAdsWebAppController sharedInstance] webView]];
  }
}

- (void)applyOptions:(NSDictionary *)options {
  [super applyOptions:options];
  
  if ([options objectForKey:kUnityAdsWebViewEventDataClickUrlKey] != nil) {
    [self openAppStoreWithData:options inViewController:[UnityAdsMainViewController sharedInstance]];
  }
}

@end
