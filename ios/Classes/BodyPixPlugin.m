#import "BodyPixPlugin.h"
#if __has_include(<body_pix/body_pix-Swift.h>)
#import <body_pix/body_pix-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "body_pix-Swift.h"
#endif

@implementation BodyPixPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBodyPixPlugin registerWithRegistrar:registrar];
}
@end
