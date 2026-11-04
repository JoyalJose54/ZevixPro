import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';
import '../widgets/window_title_bar.dart';
import 'main_screen.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with SingleTickerProviderStateMixin {
  late AnimationController _animController;
  bool _isNavigated = false;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(vsync: this);
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  void _navigateToMain() {
    if (_isNavigated) return;
    _isNavigated = true;

    if (!mounted) return;

    Navigator.of(context).pushReplacement(
      PageRouteBuilder(
        transitionDuration: const Duration(milliseconds: 400),
        pageBuilder: (context, animation, secondaryAnimation) => const MainScreen(),
        transitionsBuilder: (context, animation, secondaryAnimation, child) {
          return FadeTransition(
            opacity: animation,
            child: child,
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Column(
        children: [
          // Sleek custom titlebar for Windows window controls & dragging
          const WindowTitleBar(
            title: "Zevix Pro",
            isGreenBanner: false,
          ),

          // Splash Animation Area (Click anywhere to skip, matches Android SplashActivity)
          Expanded(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: _navigateToMain,
              child: Center(
                child: SizedBox(
                  width: 320,
                  height: 320,
                  child: Lottie.asset(
                    'assets/video_splash.json',
                    controller: _animController,
                    fit: BoxFit.contain,
                    onLoaded: (composition) {
                      _animController.duration = composition.duration;
                      _animController.forward().then((_) {
                        _navigateToMain();
                      });
                    },
                    errorBuilder: (context, error, stackTrace) {
                      // Fallback if Lottie asset fails to load
                      WidgetsBinding.instance.addPostFrameCallback((_) {
                        Future.delayed(const Duration(milliseconds: 800), _navigateToMain);
                      });
                      return Image.asset(
                        'assets/logo.png',
                        width: 140,
                        height: 140,
                        fit: BoxFit.contain,
                      );
                    },
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

