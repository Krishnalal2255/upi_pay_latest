import Flutter
import UIKit

public class SwiftUpiPayPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "upi_pay", binaryMessenger: registrar.messenger())
    let instance = SwiftUpiPayPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) -> Void {
    guard let arguments = call.arguments as? [String: Any],
          let uri = arguments["uri"] as? String else {
      result(FlutterError(code: "INVALID_ARGUMENT", message: "URI is required", details: nil))
      return
    }
    
    switch call.method {
    case "canLaunch":
      result(self.canLaunch(uri: uri))
    case "launch":
      self.launchUri(uri: uri, result: result)
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func canLaunch(uri: String) -> Bool {
    guard let url = URL(string: uri) else { return false }
    return UIApplication.shared.canOpenURL(url)
  }

  private func launchUri(uri: String, result: @escaping FlutterResult) {
    guard let url = URL(string: uri), canLaunch(uri: uri) else {
      result(false)
      return
    }
    
    if #available(iOS 10.0, *) {
      UIApplication.shared.open(url, options: [:], completionHandler: { (success) in
        result(success)
      })
    } else {
      result(UIApplication.shared.openURL(url))
    }
  }
}
